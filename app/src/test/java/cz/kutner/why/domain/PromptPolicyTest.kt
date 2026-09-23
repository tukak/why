package cz.kutner.why.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class PromptPolicyTest {
    private val now = 1_000_000L
    private val window = 3_000L

    private fun decide(
        lastLockAt: Long? = null,
        previousOpen: Boolean = false,
        previousAnswered: Boolean = false,
        pausedUntil: Long = 0,
        canShow: Boolean = true,
        inCall: Boolean = false,
        resumeWindowMs: Long = window,
    ) = PromptPolicy.decide(now, lastLockAt, resumeWindowMs, previousOpen, previousAnswered, pausedUntil, canShow, inCall)

    @Test
    fun `a fresh unlock asks why`() {
        assertEquals(UnlockDecision(resumePrevious = false, prompt = true), decide())
    }

    @Test
    fun `a quick re-unlock after an answered session does not ask again`() {
        // The screen timed out while the user was still doing what they said.
        assertEquals(UnlockDecision(resumePrevious = true, prompt = false), decide(lastLockAt = now - 2_000, previousAnswered = true))
    }

    @Test
    fun `a quick re-unlock after an unanswered session still asks`() {
        assertEquals(UnlockDecision(resumePrevious = true, prompt = true), decide(lastLockAt = now - 2_000, previousAnswered = false))
    }

    @Test
    fun `a repeated unlock signal without screen off belongs to the same session`() {
        // A new session here would split one unlock in two and replace the question under the user's finger.
        assertEquals(UnlockDecision(resumePrevious = true, prompt = false), decide(lastLockAt = now - 60_000, previousOpen = true))
    }

    @Test
    fun `an unlock after the resume window is a new session`() {
        assertEquals(
            UnlockDecision(resumePrevious = false, prompt = true),
            decide(lastLockAt = now - window - 1, previousAnswered = true),
        )
    }

    @Test
    fun `a deliberate relock of a few seconds asks again`() {
        // A 9 s power-button relock is a new decision to pick up the phone, not a screen timeout.
        assertEquals(UnlockDecision(resumePrevious = false, prompt = true), decide(lastLockAt = now - 9_000, previousAnswered = true))
    }

    @Test
    fun `with the window off, even an instant re-unlock asks`() {
        assertEquals(
            UnlockDecision(resumePrevious = false, prompt = true),
            decide(lastLockAt = now - 500, previousAnswered = true, resumeWindowMs = 0),
        )
    }

    @Test
    fun `pause keeps counting unlocks but does not ask`() {
        assertEquals(UnlockDecision(resumePrevious = false, prompt = false), decide(pausedUntil = now + 1))
    }

    @Test
    fun `the question comes back when the pause ends`() {
        assertEquals(UnlockDecision(resumePrevious = false, prompt = true), decide(pausedUntil = now))
    }

    @Test
    fun `without the overlay permission nothing is shown`() {
        assertEquals(UnlockDecision(resumePrevious = false, prompt = false), decide(canShow = false))
    }

    @Test
    fun `an unlock during a call is counted but not interrupted by the question`() {
        // The question would cover the call screen, including the hang-up button.
        assertEquals(UnlockDecision(resumePrevious = false, prompt = false), decide(inCall = true))
    }

    @Test
    fun `a clock that moved backwards starts a new session`() {
        assertEquals(UnlockDecision(resumePrevious = false, prompt = true), decide(lastLockAt = now + 1_000, previousAnswered = true))
    }
}
