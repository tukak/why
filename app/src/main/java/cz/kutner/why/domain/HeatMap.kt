package cz.kutner.why.domain

import cz.kutner.why.data.db.UnlockEvent
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

class HabitHeatMap(
    /** Habit unlocks per weekday (Monday first) and hour of day. */
    val counts: List<IntArray>,
) {
    val total: Int = counts.sumOf { it.sum() }
    val max: Int = counts.maxOf { it.max() }

    /** The hour with the most habit unlocks over all days; the earliest one on a tie. */
    val peakHour: Int? = if (total == 0) null else (0 until 24).maxBy { hour -> counts.sumOf { it[hour] } }

    fun count(day: DayOfWeek, hour: Int): Int = counts[day.value - 1][hour]
}

fun habitHeatMap(events: List<UnlockEvent>, zone: ZoneId): HabitHeatMap {
    val counts = List(7) { IntArray(24) }
    for (event in events) {
        if (event.answer != Answer.Habit) continue
        val at = Instant.ofEpochMilli(event.unlockedAt).atZone(zone)
        counts[at.dayOfWeek.value - 1][at.hour]++
    }
    return HabitHeatMap(counts)
}
