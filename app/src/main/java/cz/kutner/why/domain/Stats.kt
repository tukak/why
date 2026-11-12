package cz.kutner.why.domain

import cz.kutner.why.data.db.UnlockEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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

/** The local date of the day the moment belongs to, moving times before [dayStartMinute] to the previous day. */
fun dayDate(epochMillis: Long, zone: ZoneId, dayStartMinute: Int = 0): LocalDate {
    val at = Instant.ofEpochMilli(epochMillis).atZone(zone)
    val boundary = localTimeOfDay(dayStartMinute)
    return if (at.toLocalTime() < boundary) at.toLocalDate().minusDays(1) else at.toLocalDate()
}

/** Start of the day the moment belongs to, at the custom day start (midnight by default). */
fun startOfDay(epochMillis: Long, zone: ZoneId, dayStartMinute: Int = 0): Long =
    dayStartInstant(dayDate(epochMillis, zone, dayStartMinute), zone, dayStartMinute)

/** Start of the day [days] calendar days before the day of [epochMillis]; stays on the custom day start across daylight saving changes. */
fun daysBack(epochMillis: Long, zone: ZoneId, days: Int, dayStartMinute: Int = 0): Long =
    dayStartInstant(dayDate(epochMillis, zone, dayStartMinute).minusDays(days.toLong()), zone, dayStartMinute)

private fun localTimeOfDay(minuteOfDay: Int): LocalTime {
    val wrapped = ((minuteOfDay % MINUTES_IN_DAY) + MINUTES_IN_DAY) % MINUTES_IN_DAY
    return LocalTime.of(wrapped / 60, wrapped % 60)
}

private fun dayStartInstant(date: LocalDate, zone: ZoneId, dayStartMinute: Int): Long =
    date.atTime(localTimeOfDay(dayStartMinute)).atZone(zone).toInstant().toEpochMilli()

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

fun summarizeDay(today: List<UnlockEvent>, history: List<UnlockEvent>, now: Long, zone: ZoneId, dayStartMinute: Int = 0): DaySummary {
    val screen = today.sumOf { it.durationMillis(now) }
    return DaySummary(
        unlocks = today.size,
        habit = today.count { it.answer == Answer.Habit },
        screenMillis = screen,
        avgMillis = if (today.isEmpty()) 0 else screen / today.size,
        vsUsual = usualSoFar(history, now, zone, dayStartMinute = dayStartMinute)?.let { today.size - it },
    )
}

/** Counts only earlier days that have any data, so the days before install do not pull the average down. */
fun usualSoFar(history: List<UnlockEvent>, now: Long, zone: ZoneId, days: Int = USUAL_DAYS, dayStartMinute: Int = 0): Int? {
    val today = dayDate(now, zone, dayStartMinute)
    val sinceDayStart = now - dayStartInstant(today, zone, dayStartMinute)
    val counts = (1..days).mapNotNull { back ->
        val date = today.minusDays(back.toLong())
        val dayStart = dayStartInstant(date, zone, dayStartMinute)
        val dayEnd = dayStartInstant(date.plusDays(1), zone, dayStartMinute)
        val cutoff = dayStart + sinceDayStart
        val ofDay = history.filter { it.unlockedAt in dayStart until dayEnd }
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

fun summarizeWeek(events: List<UnlockEvent>, now: Long, zone: ZoneId, dayStartMinute: Int = 0): WeekSummary {
    val today = dayDate(now, zone, dayStartMinute)
    val byDate = events.groupBy { dayDate(it.unlockedAt, zone, dayStartMinute) }
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
