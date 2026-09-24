package cz.kutner.why.domain

import cz.kutner.why.data.db.UnlockEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReflectionTest {
    private val zone = ZoneId.of("Europe/Prague")
    private val today = LocalDate.of(2026, 9, 23)
    private val minute = 60_000L

    private fun at(date: LocalDate, hour: Int, minuteOfHour: Int = 0): Long =
        date.atTime(LocalTime.of(hour, minuteOfHour)).atZone(zone).toInstant().toEpochMilli()

    private fun event(unlockedAt: Long, habit: Boolean = false) =
        UnlockEvent(unlockedAt = unlockedAt, lockedAt = unlockedAt + 2 * minute, isHabit = habit)

    @Test
    fun `a day without unlocks sends no summary`() {
        assertNull(reflectOnDay(emptyList(), listOf(event(at(today.minusDays(1), 10))), at(today, 21), zone))
    }

    @Test
    fun `the summary counts habit unlocks and compares with earlier days by the same hour`() {
        val todays = listOf(event(at(today, 8), habit = true), event(at(today, 12)), event(at(today, 20), habit = true))
        // Each earlier day had 5 unlocks by 21:00; the ones at 22:00 happened later in the day and must not count.
        val history = (1L..2).flatMap { back -> (8..12).map { event(at(today.minusDays(back), it)) } + event(at(today.minusDays(back), 22)) }
        val reflection = reflectOnDay(todays, history, at(today, 21), zone)!!
        assertEquals(3, reflection.unlocks)
        assertEquals(2, reflection.habit)
        assertEquals(6 * minute, reflection.screenMillis)
        assertEquals(-2, reflection.vsUsual)
    }

    @Test
    fun `without history there is nothing to compare with`() {
        assertNull(reflectOnDay(listOf(event(at(today, 8))), emptyList(), at(today, 21), zone)!!.vsUsual)
    }

    @Test
    fun `before the chosen time the summary comes today, after it tomorrow`() {
        assertEquals(at(today, 21), nextTimeOfDay(at(today, 20, 59), 21 * 60, zone))
        assertEquals(at(today.plusDays(1), 21), nextTimeOfDay(at(today, 21), 21 * 60, zone))
    }

    @Test
    fun `the summary keeps its local time over a daylight saving change`() {
        // Clocks go back on 25 October 2026 in Prague; that day has 25 hours.
        val before = LocalDate.of(2026, 10, 24)
        assertEquals(at(before.plusDays(1), 21), nextTimeOfDay(at(before, 22), 21 * 60, zone))
    }
}
