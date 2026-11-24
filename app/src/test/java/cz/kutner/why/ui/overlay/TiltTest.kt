package cz.kutner.why.ui.overlay

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TiltTest {
    private val frame = 1 / 60f
    private val tenDegrees = Offset(0f, 1.7f)

    private fun hold(start: Offset, reading: Offset, seconds: Float): Offset {
        var level = start
        repeat((seconds / frame).toInt()) { level = followLevel(level, reading, frame) }
        return level
    }

    // A hand never holds the phone perfectly still; that must not move the ball.
    @Test
    fun steadyHandKeepsTheBallStill() {
        assertEquals(Offset.Zero, tiltSpeed(Offset(0.3f, -0.3f)))
    }

    // A small tilt must move the ball slowly enough to stop on the right answer.
    @Test
    fun smallTiltMovesSlowly() {
        val speed = tiltSpeed(tenDegrees).getDistance()
        assertTrue(speed in 80f..160f, "speed $speed dp/s")
    }

    @Test
    fun strongTiltIsCapped() {
        assertEquals(TILT_MAX_DP_PER_SECOND, tiltSpeed(Offset(6f, 6f)).getDistance(), 0.01f)
    }

    // Tilting to steer and then back to the start must stop the ball, not push it the other way.
    @Test
    fun goingBackToTheStartStopsTheBall() {
        val level = hold(Offset.Zero, tenDegrees, seconds = 2f)
        assertEquals(Offset.Zero, tiltSpeed(Offset.Zero - level))
    }

    // A level taken while the phone was still being raised must not pull the ball one way for good.
    @Test
    fun aWrongStartFadesOut() {
        val upright = Offset(0f, 9.81f)
        val held = Offset(0f, 6.94f)
        val level = hold(upright, held, seconds = 20f)
        assertEquals(Offset.Zero, tiltSpeed(held - level))
    }
}
