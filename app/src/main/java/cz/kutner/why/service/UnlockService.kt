package cz.kutner.why.service

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import cz.kutner.why.MainActivity
import cz.kutner.why.R
import cz.kutner.why.container
import cz.kutner.why.data.PromptChoices
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
import cz.kutner.why.ui.style
import cz.kutner.why.ui.theme.PebbleStyle
import cz.kutner.why.ui.theme.WhyTheme
import cz.kutner.why.ui.timeFormatter
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Watches unlocks for the whole time the user has the feature on. */
class UnlockService : LifecycleService() {

    private val app by lazy { applicationContext.container }
    private lateinit var overlays: OverlayController
    private val screenEvents = Channel<String>(Channel.UNLIMITED)

    private var currentId: Long? = null
    private var lastLockAt: Long? = null
    private var unlocked = false
    private var nudgeJob: Job? = null
    private var unlockWatch: Job? = null
    private var prepared: Prepared? = null
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
        getSystemService(NotificationManager::class.java).cancel(STOPPED_NOTIFICATION_ID)
        // USER_PRESENT comes from System UI, not the system uid, so a non-exported receiver never gets it.
        // All three actions are protected broadcasts: other apps cannot send them.
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(Intent.ACTION_USER_PRESENT).apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
        lifecycleScope.launch {
            app.unlocks.closeOrphanSessions()
            for (action in screenEvents) {
                when (action) {
                    Intent.ACTION_SCREEN_ON -> onScreenOn()
                    Intent.ACTION_USER_PRESENT -> {
                        unlockWatch?.cancel()
                        onUnlock()
                    }
                    UNLOCKED_EARLY -> onUnlock()
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
        val canShow = overlays.canShow()
        val decision = PromptPolicy.decide(
            now = now,
            lastLockAt = lastLockAt,
            resumeWindowMs = settings.resumeSeconds * 1000L,
            previousOpen = previous != null && previous.lockedAt == null,
            previousAnswered = previous != null && previous.answer != Answer.None,
            pausedUntil = settings.pausedUntil,
            canShowOverlay = canShow,
            inCall = inCall(),
        )
        val id = if (decision.resumePrevious && previous != null) {
            if (previous.lockedAt != null) app.unlocks.resumeSession(previous.id)
            previous.id
        } else {
            app.unlocks.startSession(now)
        }
        currentId = id
        unlocked = true
        if (canShow == showingPermissionHint) {
            showingPermissionHint = !canShow
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification())
        }
        if (decision.prompt) showPrompt(id, settings)
    }

    /**
     * Android announces the unlock only after its unlock animation, about half a second late. While the lock screen
     * shows, the question's data is loaded and the lock state is checked directly; a late announcement then finds
     * the session already open and asks nothing more.
     */
    private suspend fun onScreenOn() {
        unlockWatch?.cancel()
        val now = app.clock.millis()
        prepared = Prepared(now, loadChoices(now))
        val keyguard = getSystemService(KeyguardManager::class.java)
        unlockWatch = lifecycleScope.launch {
            withTimeoutOrNull(UNLOCK_WATCH_LIMIT) {
                while (keyguard.isKeyguardLocked) delay(UNLOCK_WATCH_STEP)
                screenEvents.trySend(UNLOCKED_EARLY)
            }
        }
    }

    private suspend fun loadChoices(now: Long): PromptChoices =
        app.unlocks.promptChoices(TimeOfDayOrder(now, app.clock.zone), answersSince = now - ORDER_HISTORY_MS)

    private suspend fun onScreenOff() {
        unlockWatch?.cancel()
        prepared = null
        nudgeJob?.cancel()
        overlays.dismiss()
        val now = app.clock.millis()
        currentId?.let { app.unlocks.closeSession(it, now) }
        // Only the end of a real session counts as a lock; a screen that showed just the lock screen ended nothing.
        if (unlocked) lastLockAt = now
        unlocked = false
    }

    private suspend fun showPrompt(id: Long, settings: AppSettings) {
        val now = app.clock.millis()
        val choices = prepared?.takeIf { now - it.at < UNLOCK_WATCH_LIMIT.inWholeMilliseconds }?.choices ?: loadChoices(now)
        prepared = null
        val unlockNumber = app.unlocks.countSince(startOfDay(now, app.clock.zone))
        val time = timeFormatter(this).format(Instant.ofEpochMilli(now).atZone(app.clock.zone))
        showThemed(settings) {
            PromptScreen(
                time = time,
                unlockNumber = unlockNumber,
                reasons = choices.reasons,
                typedBefore = choices.typed,
                onReason = { answer(id, settings, reason = it) },
                onHabit = { answer(id, settings, habit = true) },
                onOther = { answer(id, settings, text = it) },
                onOpenApp = { openApp(id) },
                onPause = {
                    lifecycleScope.launch {
                        app.settings.setPausedUntil(app.clock.millis() + 1.hours.inWholeMilliseconds)
                        overlays.dismiss()
                    }
                },
            )
        }
    }

    private fun showThemed(settings: AppSettings, content: @Composable () -> Unit) =
        overlays.show { WhyTheme(settings.themeMode, settings.dynamicColor, content = content) }

    private fun answer(id: Long, settings: AppSettings, reason: Reason? = null, habit: Boolean = false, text: String? = null) {
        lifecycleScope.launch {
            app.unlocks.answer(id, reasonId = reason?.id, isHabit = habit, customText = text)
            val offer = text?.let { app.unlocks.dueOfferFor(it) }
            if (offer != null) showOffer(offer, settings) else overlays.dismiss()
            val style = reason?.style ?: if (habit) PebbleStyle.Habit else PebbleStyle.Other
            scheduleNudge(id, settings, NudgeTarget(reason?.label ?: text, style), if (reason?.nudge == false) 0 else settings.nudgeMinutes)
        }
    }

    private fun showOffer(offer: TypedReasons.Group, settings: AppSettings) {
        showThemed(settings) {
            OfferScreen(label = offer.label, count = offer.count) { decision ->
                lifecycleScope.launch {
                    app.unlocks.decide(offer, decision)
                    overlays.dismiss()
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
            val minutes = (app.clock.millis() - event.unlockedAt).milliseconds.inWholeMinutes.toInt()
            showThemed(settings) {
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

    /** Opening this app is its own answer; the user is already looking at their phone use, so no check-in follows. */
    private fun openApp(id: Long) {
        nudgeJob?.cancel()
        lifecycleScope.launch {
            app.unlocks.answer(id, isAppCheck = true)
            overlays.dismiss()
            val open = Intent(this@UnlockService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(open) }.onFailure { Log.w(TAG, "Cannot open the app", it) }
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

    private class Prepared(val at: Long, val choices: PromptChoices)

    companion object {
        private const val TAG = "UnlockService"
        private const val CHANNEL_ID = "unlock_service"
        private const val NOTIFICATION_ID = 1
        private const val STOPPED_CHANNEL_ID = "service_stopped"
        private const val STOPPED_NOTIFICATION_ID = 3
        private const val SNOOZE_MINUTES = 5
        private const val UNLOCKED_EARLY = "cz.kutner.why.UNLOCKED_EARLY"
        private val UNLOCK_WATCH_STEP = 100.milliseconds
        private val UNLOCK_WATCH_LIMIT = 60.seconds
        private val ORDER_HISTORY_MS = 30.days.inWholeMilliseconds

        /** False when Android does not allow a start from the background right now. */
        fun start(context: Context): Boolean = try {
            ContextCompat.startForegroundService(context, Intent(context, UnlockService::class.java))
            true
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Log.w(TAG, "Cannot start service now", e)
            false
        }

        /**
         * For starts without the user, after the system stopped the app. When Android blocks the start,
         * a tap on a notification is allowed to start it.
         */
        fun ensureRunning(context: Context) {
            if (start(context)) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(STOPPED_CHANNEL_ID, context.getString(R.string.stopped_channel), NotificationManager.IMPORTANCE_LOW),
            )
            val restart = PendingIntent.getForegroundService(
                context, 0, Intent(context, UnlockService::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, STOPPED_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.stopped_title))
                .setContentText(context.getString(R.string.stopped_text))
                .setContentIntent(restart)
                .setAutoCancel(true)
                .build()
            manager.notify(STOPPED_NOTIFICATION_ID, notification)
        }
    }
}
