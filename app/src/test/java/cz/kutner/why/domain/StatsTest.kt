package cz.kutner.why.domain

import cz.kutner.why.data.db.UnlockEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatsTest {
    private val zone = ZoneId.of("Europe/Prague")
    private val today = LocalDate.of(2026, 9, 23)
    private val minute = 60_000L

    private fun at(date: LocalDate, hour: Int, minuteOfHour: Int = 0): Long =
        date.atTime(LocalTime.of(hour, minuteOfHour)).atZone(zone).toInstant().toEpochMilli()

    private fun event(unlockedAt: Long, lengthMinutes: Long? = 1, reasonId: Long? = null, habit: Boolean = false, text: String? = null) =
        UnlockEvent(
            unlockedAt = unlockedAt,
            lockedAt = lengthMinutes?.let { unlockedAt + it * minute },
            reasonId = reasonId,
            isHabit = habit,
            customText = text,
        )

    @Test
    fun `an open session counts until now`() {
        val start = at(today, 10)
        assertEquals(4 * minute, event(start, lengthMinutes = null).durationMillis(start + 4 * minute))
    }

    @Test
    fun `a session left open for hours is capped so it cannot dominate screen time`() {
        val start = at(today, 1)
        assertEquals(MAX_SESSION_MS, event(start, lengthMinutes = null).durationMillis(at(today, 23)))
    }

    @Test
    fun `usual so far compares with the same time of day on earlier days`() {
        val now = at(today, 12)
        val history = listOf(
            // Yesterday: 2 before noon, 3 after. Only the morning ones count.
            event(at(today.minusDays(1), 8)), event(at(today.minusDays(1), 11)),
            event(at(today.minusDays(1), 14)), event(at(today.minusDays(1), 18)), event(at(today.minusDays(1), 21)),
            // Two days ago: 4 before noon.
            event(at(today.minusDays(2), 7)), event(at(today.minusDays(2), 8)), event(at(today.minusDays(2), 9)), event(at(today.minusDays(2), 10)),
        )
        assertEquals(3, usualSoFar(history, now, zone))
    }

    @Test
    fun `days without any data do not pull the usual count down`() {
        // Two recorded days with gaps between them, for example right after install.
        val history = listOf(
            event(at(today.minusDays(3), 9)), event(at(today.minusDays(3), 10)),
            event(at(today.minusDays(5), 9)), event(at(today.minusDays(5), 10)),
        )
        assertEquals(2, usualSoFar(history, at(today, 12), zone))
    }

    @Test
    fun `a single earlier day is not enough to call anything usual`() {
        val history = listOf(event(at(today.minusDays(1), 9)), event(at(today.minusDays(1), 10)))
        assertNull(usualSoFar(history, at(today, 12), zone))
    }

    @Test
    fun `no history means no comparison`() {
        assertNull(usualSoFar(emptyList(), at(today, 12), zone))
    }

    @Test
    fun `week summary splits habit time from time with a reason`() {
        val events = listOf(
            event(at(today, 9), lengthMinutes = 12, habit = true),
            event(at(today, 10), lengthMinutes = 8, habit = true),
            event(at(today, 11), lengthMinutes = 2, reasonId = 1),
            event(at(today, 12), lengthMinutes = 4, text = "Pay for parking"),
            event(at(today, 13), lengthMinutes = 30),
        )
        val week = summarizeWeek(events, at(today, 23), zone)
        assertEquals(10 * minute, week.habitAvgMillis)
        assertEquals(3 * minute, week.reasonAvgMillis)
        assertEquals(20 * minute, week.timeByAnswer[Answer.Habit])
        assertEquals(7, week.days.size)
        assertEquals(today, week.days.last().date)
        assertEquals(5, week.days.last().total)
        assertEquals(2, week.days.last().habit)
    }
}
