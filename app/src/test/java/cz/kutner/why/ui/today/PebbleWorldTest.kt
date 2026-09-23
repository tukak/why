package cz.kutner.why.ui.today

import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertTrue

class PebbleWorldTest {
    private val width = 1000f
    private val height = 900f
    private val corner = 110f
    private val inset = 10f
    private val radius = 40f
    private val gravity = 9.81f * 500f

    private fun world(count: Int, r: Float = radius, perRow: Int = 8) = PebbleWorld(width, height, corner, inset, restSpeed = 80f).apply {
        repeat(count) { i -> bodies += PebbleWorld.Body(60f + (i % perRow) * (2.3f * r), 60f + (i / perRow) * (2.3f * r), r) }
    }

    private fun PebbleWorld.run(seconds: Float, ax: Float, ay: Float) {
        repeat((seconds * 60).toInt()) { step(1 / 60f, ax, ay) }
    }

    @Test
    fun `pebbles never leave the jar, even when shaken hard`() {
        val w = world(40)
        repeat(20) { i ->
            val sign = if (i % 2 == 0) 1 else -1
            w.run(0.1f, sign * gravity * 4, -sign * gravity * 3)
        }
        w.bodies.forEach {
            assertTrue(it.x - it.radius >= inset - 1 && it.x + it.radius <= width - inset + 1, "x out of jar: ${it.x}")
            assertTrue(it.y - it.radius >= inset - 1 && it.y + it.radius <= height - inset + 1, "y out of jar: ${it.y}")
        }
    }

    @Test
    fun `a small pile comes to rest on its own, before the time limit`() {
        val w = world(40)
        w.run(2.5f, 0f, gravity)
        val speeds = w.bodies.map { hypot(it.vx, it.vy) }.sorted()
        assertTrue(w.resting, "speeds after settling: median ${speeds[speeds.size / 2]}, max ${speeds.last()}")
    }

    @Test
    fun `tilting the phone to the right rolls the pile to the right`() {
        val w = world(24)
        w.run(3f, 0f, gravity)
        val before = w.bodies.map { it.x }.average()
        w.wake()
        w.run(3f, gravity * 0.7f, gravity * 0.7f)
        val after = w.bodies.map { it.x }.average()
        assertTrue(after > before + 100, "center moved from $before to $after")
    }

    @Test
    fun `settled pebbles keep their size instead of sinking into each other`() {
        val w = world(40)
        w.run(4f, 0f, gravity)
        var worst = 0f
        for (i in w.bodies.indices) for (j in i + 1 until w.bodies.size) {
            val a = w.bodies[i]
            val b = w.bodies[j]
            worst = maxOf(worst, a.radius + b.radius - hypot(b.x - a.x, b.y - a.y))
        }
        assertTrue(worst < radius * 0.25f, "deepest overlap $worst")
    }

    @Test
    fun `500 small pebbles stay in the jar, stop within the time limit and keep their size`() {
        // A heavy day: the pile must not shake forever or boil out of the jar.
        val small = 12f
        val w = world(500, r = small, perRow = 30)
        w.run(4f, 0f, gravity)
        assertTrue(w.resting)
        w.bodies.forEach { assertTrue(it.y + it.radius <= height - inset + 1 && it.y - it.radius >= inset - 1, "y out of jar: ${it.y}") }
        var worst = 0f
        for (i in w.bodies.indices) for (j in i + 1 until w.bodies.size) {
            val a = w.bodies[i]
            val b = w.bodies[j]
            worst = maxOf(worst, a.radius + b.radius - hypot(b.x - a.x, b.y - a.y))
        }
        assertTrue(worst < small * 0.5f, "deepest overlap $worst")
    }

    @Test
    fun `a size change keeps each pebble at the same relative place`() {
        val w = world(10)
        w.run(2f, 0f, gravity)
        val before = w.bodies.map { it.x / width to it.y / height }
        w.resize(width * 1.5f, height * 0.8f)
        val after = w.bodies.map { it.x / (width * 1.5f) to it.y / (height * 0.8f) }
        before.zip(after).forEach { (b, a) ->
            assertTrue(kotlin.math.abs(b.first - a.first) < 0.001f && kotlin.math.abs(b.second - a.second) < 0.001f)
        }
    }
}
