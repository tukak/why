package cz.kutner.why.domain

import cz.kutner.why.data.db.CustomEntry
import cz.kutner.why.data.db.Reason
import cz.kutner.why.data.db.TypedOffer
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypedReasonsTest {
    private fun entries(vararg texts: String) = texts.mapIndexed { i, t -> CustomEntry(id = i.toLong() + 1, text = t, unlockedAt = i * 1000L) }

    @Test
    fun `spelling differences count as the same answer`() {
        // Typing on a phone keyboard: case, diacritics, spaces and a trailing dot vary.
        val keys = listOf("Parkování", "parkovani", "  PARKOVÁNÍ. ", "parkování!").map(TypedReasons::normalize).toSet()
        assertEquals(setOf("parkovani"), keys)
    }

    @Test
    fun `the group label is the spelling used most often`() {
        val group = TypedReasons.group(entries("Parkování", "parkovani", "Parkování")).single()
        assertEquals("Parkování", group.label)
        assertEquals(3, group.count)
    }

    @Test
    fun `an answer is offered at 5, again at 10 after not now, and never after dont ask`() {
        fun group(count: Int) = TypedReasons.group(entries(*Array(count) { "Parking" })).single()
        assertFalse(TypedReasons.isDue(group(4), null))
        assertTrue(TypedReasons.isDue(group(5), null))
        val notNowAt5 = TypedOffer("parking", lastOfferedCount = 5)
        assertFalse(TypedReasons.isDue(group(9), notNowAt5))
        assertTrue(TypedReasons.isDue(group(10), notNowAt5))
        assertFalse(TypedReasons.isDue(group(15), TypedOffer("parking", lastOfferedCount = 10, never = true)))
    }

    @Test
    fun `a typed answer that already is a reason is not offered again`() {
        val groups = TypedReasons.group(entries(*Array(5) { "navigate" }))
        val reasons = listOf(Reason(id = 1, label = "Navigate", shape = "", color = "", position = 0))
        assertTrue(TypedReasons.due(groups, emptyList(), reasons).isEmpty())
    }

    @Test
    fun `suggestions filter by what is typed, ignoring case and diacritics`() {
        assertTrue(TypedReasons.matches("Parkování", "park"))
        assertTrue(TypedReasons.matches("Parkování", "ANI"))
        assertFalse(TypedReasons.matches("Parkování", "bus"))
        assertTrue(TypedReasons.matches("Parkování", ""))
    }

    @Test
    fun `suggestions list the most used earlier answers first`() {
        val groups = TypedReasons.group(entries("Bus", "Parking", "Parking", "Wordle", "Parking", "Wordle"))
        val order = TimeOfDayOrder(now = 10_000L, zone = ZoneId.of("UTC"))
        assertEquals(listOf("Parking", "Wordle", "Bus"), TypedReasons.suggestions(groups, emptyList(), order))
    }
}
