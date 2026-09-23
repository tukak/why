package cz.kutner.why.service

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.media.AudioManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import cz.kutner.why.MainActivity
import cz.kutner.why.R
import cz.kutner.why.container
import cz.kutner.why.data.db.Reason
import cz.kutner.why.data.settings.AppSettings
import cz.kutner.why.domain.Answer
import cz.kutner.why.domain.PromptPolicy
import cz.kutner.why.domain.TimeOfDayOrder
import cz.kutner.why.domain.TypedReasons
import cz.kutner.why.domain.answer
import cz.kutner.why.domain.startOfDay
import cz.kutner.why.ui.overlay.NudgeScreen
import cz.kutner.why.ui.overlay.OfferScreen
import cz.kutner.why.ui.overlay.PromptScreen
import cz.kutner.why.ui.theme.PebbleShape
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.ReasonColor
import cz.kutner.why.ui.theme.WhyTheme
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Watches unlocks for the whole time the user has the feature on. */
class UnlockService : LifecycleService() {

    private val app by lazy { applicationContext.container }
    private lateinit var overlays: OverlayController
    private val screenEvents = Channel<String>(Channel.UNLIMITED)

    private var currentId: Long? = null
    private var lastLockAt: Long? = null
    private var nudgeJob: Job? = null
    private var showingPermissionHint = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.action?.let(screenEvents::trySend)
        }
    }

    override fun onCreate() {
        super.onCreate()
        overlays = OverlayController(this)
        startInForeground()
        // USER_PRESENT comes from System UI, not the system uid, so a non-exported receiver never gets it.
        // Both actions are protected broadcasts: other apps cannot send them.
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_USER_PRESENT).apply { addAction(Intent.ACTION_SCREEN_OFF) },
            ContextCompat.RECEIVER_EXPORTED,
        )
        lifecycleScope.launch {
            app.unlocks.closeOrphanSessions()
            for (action in screenEvents) {
                when (action) {
                    Intent.ACTION_USER_PRESENT -> onUnlock()
                    Intent.ACTION_SCREEN_OFF -> onScreenOff()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        overlays.dismiss()
        super.onDestroy()
    }

    private suspend fun onUnlock() {
        val now = app.clock.millis()
        val settings = app.settings.current()
        val previous = currentId?.let { app.unlocks.event(it) }
        val decision = PromptPolicy.decide(
            now = now,
            lastLockAt = lastLockAt,
            resumeWindowMs = settings.resumeSeconds * 1000L,
            previousOpen = previous != null && previous.lockedAt == null,
            previousAnswered = previous != null && previous.answer != Answer.None,
            pausedUntil = settings.pausedUntil,
            canShowOverlay = overlays.canShow(),
            inCall = inCall(),
        )
        val id = if (decision.resumePrevious && previous != null) {
            if (previous.lockedAt != null) app.unlocks.resumeSession(previous.id)
            previous.id
        } else {
            app.unlocks.startSession(now)
        }
        currentId = id
        val canShow = overlays.canShow()
        if (canShow == showingPermissionHint) {
            showingPermissionHint = !canShow
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification())
        }
        if (decision.prompt) showPrompt(id, settings)
    }

    private suspend fun onScreenOff() {
        nudgeJob?.cancel()
        overlays.dismiss()
        val now = app.clock.millis()
        currentId?.let { app.unlocks.closeSession(it, now) }
        lastLockAt = now
    }

    private suspend fun showPrompt(id: Long, settings: AppSettings) {
        val now = app.clock.millis()
        val choices = app.unlocks.promptChoices(TimeOfDayOrder(now, app.clock.zone), answersSince = now - ORDER_HISTORY_MS)
        val unlockNumber = app.unlocks.countSince(startOfDay(now, app.clock.zone))
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(now).atZone(app.clock.zone))
        overlays.show {
            WhyTheme(settings.themeMode, settings.dynamicColor) {
                PromptScreen(
                    time = time,
                    unlockNumber = unlockNumber,
                    reasons = choices.reasons,
                    typedBefore = choices.typed,
                    onReason = { answer(id, settings, reason = it) },
                    onHabit = { answer(id, settings, habit = true) },
                    onOther = { answer(id, settings, text = it) },
                    onPause = {
                        lifecycleScope.launch {
                            app.settings.setPausedUntil(app.clock.millis() + 1.hours.inWholeMilliseconds)
                            overlays.dismiss()
                        }
                    },
                )
            }
        }
    }

    private fun answer(id: Long, settings: AppSettings, reason: Reason? = null, habit: Boolean = false, text: String? = null) {
        lifecycleScope.launch {
            app.unlocks.answer(id, reasonId = reason?.id, isHabit = habit, customText = text)
            val offer = text?.let { app.unlocks.dueOfferFor(it) }
            if (offer != null) showOffer(offer, settings) else overlays.dismiss()
            val style = when {
                reason != null -> PebbleStyle(PebbleShape.of(reason.shape), ReasonColor.of(reason.color))
                habit -> PebbleStyle.Habit
                else -> PebbleStyle.Other
            }
            scheduleNudge(id, settings, NudgeTarget(reason?.label ?: text, style), settings.nudgeMinutes)
        }
    }

    private fun showOffer(offer: TypedReasons.Group, settings: AppSettings) {
        overlays.show {
            WhyTheme(settings.themeMode, settings.dynamicColor) {
                OfferScreen(label = offer.label, count = offer.count) { decision ->
                    lifecycleScope.launch {
                        app.unlocks.decide(offer, decision)
                        overlays.dismiss()
                    }
                }
            }
        }
    }

    private fun scheduleNudge(id: Long, settings: AppSettings, target: NudgeTarget, afterMinutes: Int) {
        nudgeJob?.cancel()
        if (afterMinutes <= 0) return
        nudgeJob = lifecycleScope.launch {
            delay(afterMinutes.minutes)
            val event = app.unlocks.event(id) ?: return@launch
            if (currentId != id || event.lockedAt != null) return@launch
            if (inCall()) {
                scheduleNudge(id, settings, target, SNOOZE_MINUTES)
                return@launch
            }
            val minutes = ((app.clock.millis() - event.unlockedAt) / 60_000).toInt()
            overlays.show {
                WhyTheme(settings.themeMode, settings.dynamicColor) {
                    NudgeScreen(
                        minutes = minutes,
                        reasonLabel = target.label,
                        style = target.style,
                        onDone = {
                            goHome()
                            overlays.dismiss()
                        },
                        onMore = {
                            overlays.dismiss()
                            scheduleNudge(id, settings, target, SNOOZE_MINUTES)
                        },
                        onSwitched = { lifecycleScope.launch { showPrompt(id, settings) } },
                    )
                }
            }
        }
    }

    /** Phone and internet calls, including one that is ringing. Needs no permission. */
    private fun inCall(): Boolean = getSystemService(AudioManager::class.java).mode in buildSet {
        add(AudioManager.MODE_IN_CALL)
        add(AudioManager.MODE_IN_COMMUNICATION)
        add(AudioManager.MODE_RINGTONE)
        add(AudioManager.MODE_CALL_SCREENING)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(AudioManager.MODE_CALL_REDIRECT)
            add(AudioManager.MODE_COMMUNICATION_REDIRECT)
        }
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(home) }.onFailure { Log.w(TAG, "Cannot open home screen", it) }
    }

    // ServiceCompat drops foreground service types the running Android version does not know.
    @SuppressLint("InlinedApi")
    private fun startInForeground() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.service_channel), NotificationManager.IMPORTANCE_MIN),
        )
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.service_title))
            .setContentText(getString(if (showingPermissionHint) R.string.service_text_no_overlay else R.string.service_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private data class NudgeTarget(val label: String?, val style: PebbleStyle)

    companion object {
        private const val TAG = "UnlockService"
        private const val CHANNEL_ID = "unlock_service"
        private const val NOTIFICATION_ID = 1
        private const val SNOOZE_MINUTES = 5
        private const val ORDER_HISTORY_MS = 30 * 24 * 60 * 60 * 1000L

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, UnlockService::class.java))
            } catch (e: ForegroundServiceStartNotAllowedException) {
                Log.w(TAG, "Cannot start service now", e)
            }
        }
    }
}
