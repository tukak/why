package cz.kutner.why.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FastAnswersTest {
    private val now = 1_000_000L

    private fun answers(fast: Int, slow: Int) = List(fast) { 400L } + List(slow) { 2_500L }

    private fun offer(
        latestMs: Long = 400,
        recent: List<Long> = answers(fast = 25, slow = 5),
        annoyingMode: Boolean = false,
        offerAfter: Long = 0,
    ) = FastAnswers.offer(latestMs, recent, annoyingMode, offerAfter, now)

    @Test
    fun `mostly fast answers offer annoying mode and say how many were fast`() {
        assertEquals(25, offer())
    }

    @Test
    fun `a few fast answers are normal and offer nothing`() {
        // Some reasons are clear before the screen shows; only a steady pattern means habit.
        assertNull(offer(recent = answers(fast = 19, slow = 11)))
    }

    @Test
    fun `habit taps on a slower phone still count as fast`() {
        // The time includes the unlock animation, so a habit tap measures about 0.8-1.3 s.
        assertEquals(30, offer(latestMs = 1_250, recent = List(30) { 1_250L }))
        assertNull(offer(latestMs = 1_300, recent = List(30) { 1_300L }))
    }

    @Test
    fun `too few answers to judge offer nothing`() {
        // A new user taps fast while learning the screen; that is not habit yet.
        assertNull(offer(recent = answers(fast = 29, slow = 0)))
    }

    @Test
    fun `only the most recent answers count`() {
        // A user who already slowed down must not get the offer for old taps.
        val recent = answers(fast = 0, slow = 30) + answers(fast = 100, slow = 0)
        assertNull(offer(recent = recent))
    }

    @Test
    fun `the offer comes only right after a fast answer`() {
        // After a slow, thoughtful answer the message would not match what the user just did.
        assertNull(offer(latestMs = 2_000))
    }

    @Test
    fun `a user who already has annoying mode on is not asked`() {
        assertNull(offer(annoyingMode = true))
    }

    @Test
    fun `not now waits until the given time`() {
        assertNull(offer(offerAfter = now + 1))
        assertEquals(25, offer(offerAfter = now))
    }

    @Test
    fun `never stops the offer for good`() {
        assertNull(offer(offerAfter = Long.MAX_VALUE))
    }
}
