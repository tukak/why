package cz.kutner.why.domain

import cz.kutner.why.data.db.UnlockEvent
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Sessions left open (for example after a crash) must not inflate screen time. */
const val MAX_SESSION_MS = 3 * 60 * 60 * 1000L

fun UnlockEvent.durationMillis(now: Long): Long =
    ((lockedAt ?: now) - unlockedAt).coerceIn(0, MAX_SESSION_MS)

sealed interface Answer {
    data object Habit : Answer
    data class Picked(val reasonId: Long) : Answer
    data object Other : Answer
    data object AppCheck : Answer
    data object None : Answer
}

val UnlockEvent.answer: Answer
    get() = when {
        isAppCheck -> Answer.AppCheck
        isHabit -> Answer.Habit
        reasonId != null -> Answer.Picked(reasonId)
        !customText.isNullOrBlank() -> Answer.Other
        else -> Answer.None
    }

fun startOfDay(epochMillis: Long, zone: ZoneId): Long = daysBack(epochMillis, zone, 0)

/** Local midnight [days] calendar days before the day of [epochMillis]; stays on midnight across daylight saving changes. */
fun daysBack(epochMillis: Long, zone: ZoneId, days: Int): Long =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().minusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

/** Earlier days that "usual" is averaged over. */
const val USUAL_DAYS = 7

/** One earlier day is an anecdote, not a habit. */
const val MIN_USUAL_DAYS = 2

data class DaySummary(
    val unlocks: Int,
    val habit: Int,
    val screenMillis: Long,
    val avgMillis: Long,
    /** Unlocks minus the average of earlier days by this time of day; null without history. */
    val vsUsual: Int?,
)

fun summarizeDay(today: List<UnlockEvent>, history: List<UnlockEvent>, now: Long, zone: ZoneId): DaySummary {
    val screen = today.sumOf { it.durationMillis(now) }
    return DaySummary(
        unlocks = today.size,
        habit = today.count { it.answer == Answer.Habit },
        screenMillis = screen,
        avgMillis = if (today.isEmpty()) 0 else screen / today.size,
        vsUsual = usualSoFar(history, now, zone)?.let { today.size - it },
    )
}

/** Counts only earlier days that have any data, so the days before install do not pull the average down. */
fun usualSoFar(history: List<UnlockEvent>, now: Long, zone: ZoneId, days: Int = USUAL_DAYS): Int? {
    val nowZ = Instant.ofEpochMilli(now).atZone(zone)
    val sinceMidnight = Duration.between(nowZ.toLocalDate().atStartOfDay(zone), nowZ)
    val counts = (1..days).mapNotNull { back ->
        val dayStart = nowZ.toLocalDate().minusDays(back.toLong()).atStartOfDay(zone)
        val dayEnd = dayStart.plusDays(1).toInstant().toEpochMilli()
        val start = dayStart.toInstant().toEpochMilli()
        val cutoff = dayStart.plus(sinceMidnight).toInstant().toEpochMilli()
        val ofDay = history.filter { it.unlockedAt in start until dayEnd }
        if (ofDay.isEmpty()) null else ofDay.count { it.unlockedAt < cutoff }
    }
    return if (counts.size < MIN_USUAL_DAYS) null else counts.average().roundToInt()
}

data class DayBar(val date: LocalDate, val total: Int, val habit: Int)

data class WeekSummary(
    val days: List<DayBar>,
    val habitAvgMillis: Long?,
    val reasonAvgMillis: Long?,
    val timeByAnswer: Map<Answer, Long>,
)

fun summarizeWeek(events: List<UnlockEvent>, now: Long, zone: ZoneId): WeekSummary {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val byDate = events.groupBy { Instant.ofEpochMilli(it.unlockedAt).atZone(zone).toLocalDate() }
    val days = (6 downTo 0).map { back ->
        val date = today.minusDays(back.toLong())
        val ofDay = byDate[date].orEmpty()
        DayBar(date, ofDay.size, ofDay.count { it.isHabit })
    }
    val habit = events.filter { it.answer == Answer.Habit }
    val withReason = events.filter { it.answer is Answer.Picked || it.answer == Answer.Other || it.answer == Answer.AppCheck }
    return WeekSummary(
        days = days,
        habitAvgMillis = habit.averageMillis(now),
        reasonAvgMillis = withReason.averageMillis(now),
        timeByAnswer = events.groupBy { it.answer }.mapValues { (_, list) -> list.sumOf { it.durationMillis(now) } },
    )
}

private fun List<UnlockEvent>.averageMillis(now: Long): Long? =
    if (isEmpty()) null else sumOf { it.durationMillis(now) } / size
