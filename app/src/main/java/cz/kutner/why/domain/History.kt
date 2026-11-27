package cz.kutner.why.domain

import cz.kutner.why.data.db.UnlockEvent
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

fun weekStart(date: LocalDate, firstDay: DayOfWeek): LocalDate = date.with(TemporalAdjusters.previousOrSame(firstDay))

fun localDate(epochMillis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

fun startMillis(date: LocalDate, zone: ZoneId): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

/** One calendar week. [days] counts only recorded days: the first week from the first session, this week up to today. */
data class WeekTotals(val start: LocalDate, val unlocks: Int, val habit: Int, val screenMillis: Long, val days: Int) {
    val perDay: Double get() = unlocks / days.toDouble()
    val habitShare: Double get() = if (unlocks == 0) 0.0 else habit / unlocks.toDouble()
    val screenPerDay: Double get() = screenMillis / days.toDouble()
}

/** Every calendar week from the one of [firstUnlock] to the one of [now], oldest first; weeks without sessions are zero. */
fun weeklyTotals(events: List<UnlockEvent>, firstUnlock: Long, now: Long, zone: ZoneId, firstDay: DayOfWeek): List<WeekTotals> {
    val firstDate = localDate(firstUnlock, zone)
    val today = localDate(now, zone)
    val byWeek = events.groupBy { weekStart(localDate(it.unlockedAt, zone), firstDay) }
    return generateSequence(weekStart(firstDate, firstDay)) { it.plusWeeks(1) }
        .takeWhile { !it.isAfter(today) }
        .map { start ->
            val week = byWeek[start].orEmpty()
            val days = ChronoUnit.DAYS.between(maxOf(start, firstDate), minOf(start.plusDays(6), today)) + 1
            WeekTotals(
                start = start,
                unlocks = week.size,
                habit = week.count { it.answer == Answer.Habit },
                screenMillis = week.sumOf { it.durationMillis(now) },
                days = days.toInt(),
            )
        }
        .toList()
}

enum class Trend { Better, Same, Worse }

data class HistoryTrends(val unlocks: Trend, val habit: Trend, val screen: Trend)

/** Below these changes a trend counts as the same: weekly numbers move this much without any real change. */
private const val SAME_RELATIVE = 0.10
private const val SAME_SHARE_POINTS = 0.03

/** Lower is better for all three. */
fun trends(first: WeekTotals, last: WeekTotals): HistoryTrends {
    fun relative(from: Double, to: Double): Trend = when {
        from == 0.0 -> if (to == 0.0) Trend.Same else Trend.Worse
        abs(to - from) / from < SAME_RELATIVE -> Trend.Same
        to < from -> Trend.Better
        else -> Trend.Worse
    }
    val shareChange = last.habitShare - first.habitShare
    return HistoryTrends(
        unlocks = relative(first.perDay, last.perDay),
        habit = when {
            abs(shareChange) < SAME_SHARE_POINTS -> Trend.Same
            shareChange < 0 -> Trend.Better
            else -> Trend.Worse
        },
        screen = relative(first.screenPerDay, last.screenPerDay),
    )
}

/**
 * Unlocks in the week at [start] minus the week before. A week still running is compared up to the same weekday and time.
 * Null when recording started after the week before began, because a partly recorded week is no fair baseline.
 */
fun weekChange(events: List<UnlockEvent>, start: LocalDate, firstUnlock: Long, now: Long, zone: ZoneId): Int? {
    val previous = start.minusWeeks(1)
    if (localDate(firstUnlock, zone).isAfter(previous)) return null
    val end = startMillis(start.plusWeeks(1), zone)
    val cutoff = if (now < end) Instant.ofEpochMilli(now).atZone(zone).minusWeeks(1).toInstant().toEpochMilli() else startMillis(start, zone)
    val current = events.count { it.unlockedAt in startMillis(start, zone) until minOf(now, end) }
    val before = events.count { it.unlockedAt in startMillis(previous, zone) until cutoff }
    return current - before
}
