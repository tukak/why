package cz.kutner.why.domain

import cz.kutner.why.data.db.Reason
import cz.kutner.why.data.db.UnlockEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryTest {
    private val zone = ZoneId.of("Europe/Prague")
    private val minute = 60_000L

    // Wednesday.
    private val today = LocalDate.of(2026, 11, 25)
    private val monday = LocalDate.of(2026, 11, 23)

    private fun at(date: LocalDate, hour: Int): Long = date.atTime(LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli()

    private fun event(unlockedAt: Long, minutes: Long = 1, habit: Boolean = false) =
        UnlockEvent(unlockedAt = unlockedAt, lockedAt = unlockedAt + minutes * minute, isHabit = habit)

    private fun week(unlocks: Int, habit: Int = 0, screenMinutes: Long = 0, days: Int = 7) =
        WeekTotals(monday, unlocks, habit, screenMinutes * minute, days)

    @Test
    fun `the week starts on the first day of the phone's region`() {
        assertEquals(monday, weekStart(today, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 11, 22), weekStart(today, DayOfWeek.SUNDAY))
        assertEquals(monday, weekStart(monday, DayOfWeek.MONDAY))
    }

    @Test
    fun `per-day values count only recorded days, so a short first week and this week are not diluted`() {
        // Started on Thursday two weeks ago; 3 sessions a day on every recorded day.
        val start = monday.minusWeeks(2).plusDays(3)
        val days = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()
        val events = days.flatMap { day -> listOf(9, 12, 18).map { event(at(day, it)) } }
        val weeks = weeklyTotals(events, events.first().unlockedAt, at(today, 20), zone, DayOfWeek.MONDAY)

        assertEquals(listOf(monday.minusWeeks(2), monday.minusWeeks(1), monday), weeks.map { it.start })
        assertEquals(listOf(4, 7, 3), weeks.map { it.days })
        assertEquals(listOf(3.0, 3.0, 3.0), weeks.map { it.perDay })
    }

    @Test
    fun `a week without sessions stays on the shelf as an empty week`() {
        val events = listOf(event(at(monday.minusWeeks(2), 9)), event(at(today, 9)))
        val weeks = weeklyTotals(events, events.first().unlockedAt, at(today, 20), zone, DayOfWeek.MONDAY)
        assertEquals(listOf(1, 0, 1), weeks.map { it.unlocks })
    }

    @Test
    fun `habit share counts against all sessions, answered or not`() {
        val events = listOf(event(at(today, 9), habit = true), event(at(today, 10)), event(at(today, 11)), event(at(today, 12)))
        val week = weeklyTotals(events, events.first().unlockedAt, at(today, 20), zone, DayOfWeek.MONDAY).single()
        assertEquals(0.25, week.habitShare)
    }

    @Test
    fun `fewer unlocks, less habit and less screen time are all better`() {
        val trends = trends(week(70, habit = 21, screenMinutes = 700), week(49, habit = 7, screenMinutes = 490))
        assertEquals(HistoryTrends(Trend.Better, Trend.Better, Trend.Better), trends)
    }

    @Test
    fun `small weekly moves count as the same, so noise does not read as progress`() {
        // 9 % fewer unlocks, 2 points less habit, 5 % more screen time.
        val trends = trends(week(100, habit = 30, screenMinutes = 1000), week(91, habit = 26, screenMinutes = 1050))
        assertEquals(HistoryTrends(Trend.Same, Trend.Same, Trend.Same), trends)
    }

    @Test
    fun `trends can go different ways`() {
        val trends = trends(week(100, habit = 20, screenMinutes = 1000), week(80, habit = 32, screenMinutes = 1200))
        assertEquals(HistoryTrends(Trend.Better, Trend.Worse, Trend.Worse), trends)
    }

    @Test
    fun `this week is compared with last week only up to the same weekday and time`() {
        val lastWeek = monday.minusWeeks(1)
        val events = listOf(
            event(at(lastWeek.minusDays(1), 9)),
            // Last week by Wednesday 12:00: 2 sessions. Later ones do not count yet.
            event(at(lastWeek, 9)), event(at(lastWeek.plusDays(2), 11)), event(at(lastWeek.plusDays(2), 15)), event(at(lastWeek.plusDays(5), 9)),
            // This week so far: 5 sessions.
            event(at(monday, 8)), event(at(monday, 9)), event(at(monday, 10)), event(at(today, 8)), event(at(today, 11)),
        )
        assertEquals(3, weekChange(events, monday, events.first().unlockedAt, at(today, 12), zone))
    }

    @Test
    fun `a past week is compared with the whole week before`() {
        val lastWeek = monday.minusWeeks(1)
        val before = monday.minusWeeks(2)
        val events = listOf(event(at(before, 9)), event(at(before.plusDays(6), 22)), event(at(lastWeek, 9)))
        assertEquals(-1, weekChange(events, lastWeek, events.first().unlockedAt, at(today, 12), zone))
    }

    @Test
    fun `no comparison when the week before was only partly recorded`() {
        val events = listOf(event(at(monday.minusWeeks(1).plusDays(3), 9)), event(at(today, 9)))
        assertNull(weekChange(events, monday, events.first().unlockedAt, at(today, 12), zone))
    }

    @Test
    fun `the export has one row per session with the answer and its words`() {
        val reasons = mapOf(1L to Reason(id = 1, label = "Jízdní řády, vlaky", shape = "Pill", color = "Blue"))
        val start = at(today, 8)
        val events = listOf(
            UnlockEvent(unlockedAt = start, lockedAt = start + 94_000, reasonId = 1),
            UnlockEvent(unlockedAt = start + minute, lockedAt = start + 2 * minute, isHabit = true),
            UnlockEvent(unlockedAt = start + 2 * minute, lockedAt = start + 3 * minute, customText = "Say \"hi\""),
            UnlockEvent(unlockedAt = start + 3 * minute),
        )
        val csv = StringBuilder().also { writeHistoryCsv(it, events, reasons, zone) }.toString()
        val expected = listOf(
            "unlocked_at,seconds,answer,reason",
            "2026-11-25T08:00:00+01:00,94,reason,\"Jízdní řády, vlaky\"",
            "2026-11-25T08:01:00+01:00,60,habit,",
            "2026-11-25T08:02:00+01:00,60,typed,\"Say \"\"hi\"\"\"",
            "2026-11-25T08:03:00+01:00,,none,",
        )
        assertEquals(expected.joinToString("\n", postfix = "\n"), csv)
    }
}
