package cz.kutner.why.ui.overlay

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertTrue

class TiltTest {
    private val upright = Offset(0f, 9.81f)
    private val held = Offset(0f, 6.94f)
    private val frame = 1 / 60f

    private fun hold(start: Offset, reading: Offset, seconds: Float): Offset {
        var level = start
        repeat((seconds / frame).toInt()) { level = followLevel(level, reading, frame) }
        return level
    }

    // A level taken at the wrong moment (phone still upright, or before the sensor answers) must not pull the ball one way for good.
    @Test
    fun heldAngleBecomesLevel() {
        val level = hold(upright, held, seconds = 6f)
        assertTrue((held - level).getDistance() < 0.2f, "left pull ${(held - level).getDistance()}")
    }

    // Tilting the phone must still move the ball: the level must not jump to a new angle at once.
    @Test
    fun newTiltStillPullsForAWhile() {
        val level = hold(held, upright, seconds = 0.5f)
        assertTrue((upright - level).getDistance() > 0.6f * (upright - held).getDistance())
    }
}
