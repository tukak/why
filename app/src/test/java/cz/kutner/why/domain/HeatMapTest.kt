package cz.kutner.why.domain

import cz.kutner.why.data.db.UnlockEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HeatMapTest {
    private val zone = ZoneId.of("Europe/Prague")
    private val wednesday = LocalDate.of(2026, 9, 23)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0): Long =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    private fun habit(at: Long) = UnlockEvent(unlockedAt = at, isHabit = true)

    @Test
    fun `only habit unlocks count, placed by local weekday and hour`() {
        val map = habitHeatMap(
            listOf(
                habit(at(wednesday, 21, 5)),
                habit(at(wednesday, 21, 55)),
                habit(at(wednesday.plusDays(4), 7)),
                UnlockEvent(unlockedAt = at(wednesday, 21, 30), reasonId = 1),
                UnlockEvent(unlockedAt = at(wednesday, 21, 40)),
            ),
            zone,
        )
        assertEquals(2, map.count(DayOfWeek.WEDNESDAY, 21))
        assertEquals(1, map.count(DayOfWeek.SUNDAY, 7))
        assertEquals(3, map.total)
        assertEquals(2, map.max)
    }

    @Test
    fun `the peak hour sums all weekdays, so a daily habit beats one busy evening`() {
        val everyMorning = (0L until 5).map { habit(at(wednesday.minusDays(it), 8)) }
        val oneEvening = (0 until 4).map { habit(at(wednesday, 22, it * 10)) }
        assertEquals(8, habitHeatMap(everyMorning + oneEvening, zone).peakHour)
    }

    @Test
    fun `without habit unlocks there is no peak`() {
        assertNull(habitHeatMap(listOf(UnlockEvent(unlockedAt = at(wednesday, 9), reasonId = 1)), zone).peakHour)
    }
}
