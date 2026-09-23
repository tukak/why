package cz.kutner.why.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cz.kutner.why.MainActivity
import cz.kutner.why.R
import cz.kutner.why.container
import cz.kutner.why.data.settings.AppSettings
import cz.kutner.why.domain.nextTimeOfDay
import cz.kutner.why.domain.reflectOnDay
import cz.kutner.why.domain.startOfDay
import cz.kutner.why.ui.formatDuration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** One quiet notification a day with the day's numbers. */
object EveningReflection {
    private const val CHANNEL_ID = "evening_reflection"
    private const val NOTIFICATION_ID = 2
    private const val WINDOW_MS = 10 * 60 * 1000L
    private const val HISTORY_MS = 7 * 24 * 60 * 60 * 1000L

    /** An inexact window needs no exact-alarm permission; a few minutes late is fine for a summary. */
    fun schedule(context: Context, settings: AppSettings) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = alarmIntent(context)
        if (!settings.reflectionEnabled) {
            alarms.cancel(intent)
            return
        }
        val clock = context.container.clock
        val at = nextTimeOfDay(clock.millis(), settings.reflectionMinute, clock.zone)
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, intent)
    }

    suspend fun post(context: Context) {
        val app = context.container
        val now = app.clock.millis()
        val dayStart = startOfDay(now, app.clock.zone)
        val (today, history) = app.unlocks.eventsSince(dayStart - HISTORY_MS).first().partition { it.unlockedAt >= dayStart }
        val day = reflectOnDay(today, history, now, app.clock.zone) ?: return
        val res = context.resources
        val numbers = listOf(
            res.getQuantityString(R.plurals.reflection_unlocks, day.unlocks, day.unlocks),
            res.getQuantityString(R.plurals.reflection_habit, day.habit, day.habit),
            res.getString(R.string.reflection_screen, formatDuration(res, day.screenMillis)),
        ).joinToString(" · ")
        val comparison = when {
            day.vsUsual == null -> null
            day.vsUsual < 0 -> res.getQuantityString(R.plurals.today_fewer, -day.vsUsual, -day.vsUsual)
            day.vsUsual > 0 -> res.getQuantityString(R.plurals.today_more, day.vsUsual, day.vsUsual)
            else -> res.getString(R.string.reflection_as_usual)
        }
        val text = listOfNotNull(numbers, comparison).joinToString("\n")

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, res.getString(R.string.reflection_channel), NotificationManager.IMPORTANCE_LOW),
        )
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(res.getString(R.string.reflection_title))
            .setContentText(numbers)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReflectionReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class ReflectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.container
        app.applicationScope.launch {
            try {
                val settings = app.settings.current()
                if (settings.reflectionEnabled) EveningReflection.post(context)
                EveningReflection.schedule(context, settings)
            } finally {
                pending.finish()
            }
        }
    }
}
