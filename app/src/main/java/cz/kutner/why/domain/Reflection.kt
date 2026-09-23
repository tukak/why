package cz.kutner.why.domain

import cz.kutner.why.data.db.UnlockEvent
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class DayReflection(
    val unlocks: Int,
    val habit: Int,
    val screenMillis: Long,
    /** Unlocks minus the usual count by this time of day; null without history. */
    val vsUsual: Int?,
)

/** Null on a day without unlocks: there is nothing to reflect on. */
fun reflectOnDay(today: List<UnlockEvent>, history: List<UnlockEvent>, now: Long, zone: ZoneId): DayReflection? {
    if (today.isEmpty()) return null
    val summary = summarizeDay(today, history, now, zone)
    return DayReflection(
        unlocks = summary.unlocks,
        habit = today.count { it.answer == Answer.Habit },
        screenMillis = summary.screenMillis,
        vsUsual = summary.usualSoFar?.let { summary.unlocks - it },
    )
}

/** The next moment at [minuteOfDay] local time, strictly after [now]. */
fun nextTimeOfDay(now: Long, minuteOfDay: Int, zone: ZoneId): Long {
    val nowZ = Instant.ofEpochMilli(now).atZone(zone)
    val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    val today = nowZ.toLocalDate().atTime(time).atZone(zone)
    val next = if (today.isAfter(nowZ)) today else nowZ.toLocalDate().plusDays(1).atTime(time).atZone(zone)
    return next.toInstant().toEpochMilli()
}
