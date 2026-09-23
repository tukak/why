package cz.kutner.why.domain

import cz.kutner.why.data.db.Reason
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeOfDayOrderTest {
    private val zone = ZoneId.of("Europe/Prague")
    private val tuesday = LocalDate.of(2026, 9, 22)
    private val saturday = LocalDate.of(2026, 9, 26)

    private val calendar = Reason(id = 1, label = "Calendar", shape = "", color = "", position = 0)
    private val music = Reason(id = 2, label = "Music", shape = "", color = "", position = 1)
    private val photo = Reason(id = 3, label = "Photo", shape = "", color = "", position = 2)
    private val reasons = listOf(calendar, music, photo)

    private fun at(date: LocalDate, hour: Int, minute: Int = 0) =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    /** Ten weekday mornings of calendar, ten weekday evenings of music, a few weekend photos. */
    private val history = (1..10L).flatMap { back ->
        val day = tuesday.minusDays(back).let { if (it.dayOfWeek.value >= 6) it.minusDays(2) else it }
        listOf(calendar.id to at(day, 8), music.id to at(day, 22))
    } + (1..10).map { photo.id to at(saturday.minusDays(7), 10, it) }

    private fun orderAt(time: Long) = TimeOfDayOrder(time, zone).order(reasons, history).map { it.label }

    @Test
    fun `a weekday morning puts the morning reason first`() {
        assertEquals("Calendar", orderAt(at(tuesday, 8, 20)).first())
    }

    @Test
    fun `a weekday evening puts the evening reason first`() {
        assertEquals("Music", orderAt(at(tuesday, 21, 50)).first())
    }

    @Test
    fun `a weekend morning follows weekend habits, not weekday ones`() {
        // Weekend mornings differ from work mornings; the calendar reason should not win here.
        assertEquals("Photo", orderAt(at(saturday, 10, 30)).first())
    }

    @Test
    fun `the window wraps around midnight`() {
        val late = listOf(music.id to at(tuesday.minusDays(1), 23, 50)) + (1..10).map { calendar.id to at(tuesday.minusDays(1), 12) }
        val order = TimeOfDayOrder(at(tuesday, 0, 30), zone, minSample = 1).order(reasons, late)
        assertEquals("Music", order.first().label)
    }

    @Test
    fun `with too little history it falls back to overall use, then to creation order`() {
        val few = listOf(photo.id to at(tuesday.minusDays(1), 15), photo.id to at(tuesday.minusDays(2), 15))
        assertEquals(listOf("Photo", "Calendar", "Music"), TimeOfDayOrder(at(tuesday, 8), zone).order(reasons, few).map { it.label })
        assertEquals(listOf("Calendar", "Music", "Photo"), TimeOfDayOrder(at(tuesday, 8), zone).order(reasons, emptyList()).map { it.label })
    }
}
