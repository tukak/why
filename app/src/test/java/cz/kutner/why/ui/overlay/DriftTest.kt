package cz.kutner.why.ui.overlay

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class DriftTest {
    // The drift animation restarts from t = 0 after t = 2π. Any gap between the two ends is a visible jump of the answer.
    @Test
    fun answerDoesNotJumpWhenTheLoopRestarts() {
        listOf(0f, 0.7f, 2.1f, 4.4f, 6.2f).forEach { phase ->
            val (startX, startY) = driftOffset(0f, phase)
            val (endX, endY) = driftOffset(2f * PI.toFloat(), phase)
            assertEquals(startX, endX, 1e-4f, "x at phase $phase")
            assertEquals(startY, endY, 1e-4f, "y at phase $phase")
        }
    }
}
