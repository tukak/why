package cz.kutner.why.ui.today

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlatPullTest {
    @Test
    fun `a phone lying flat still pulls the pebbles gently down`() {
        assertEquals(4f, flatPull(0f, 0f))
    }

    @Test
    fun `an upright or tilted phone keeps its real gravity, so tilting feels unchanged`() {
        assertEquals(0f, flatPull(0f, 9.81f))
        assertEquals(0f, flatPull(-7f, 7f))
        // Upside down the pebbles must still fall toward the top, not be pushed back.
        assertEquals(0f, flatPull(0f, -9.81f))
    }

    @Test
    fun `the extra pull fades out smoothly while the phone is being lifted`() {
        assertTrue(flatPull(0f, 1f) > flatPull(0f, 2f))
        assertTrue(flatPull(0f, 2f) > flatPull(0f, 3f))
    }
}
