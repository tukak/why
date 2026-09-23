package cz.kutner.why.ui.today

import cz.kutner.why.ui.theme.PebbleShape
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertTrue

class JarWorldTest {
    private val width = 1000f
    private val height = 900f
    private val gravity = 9.81f * 1100f

    private fun jar(count: Int, size: Float = 80f, perRow: Int = 8) = JarWorld(width, height, cornerRadius = 110f, inset = 10f).apply {
        val shapes = PebbleShape.entries
        repeat(count) { i ->
            add(60f + (i % perRow) * size * 1.1f, 60f + (i / perRow) * size * 1.1f, size, 0f, shapes[i % shapes.size])
        }
        wake()
    }

    private fun JarWorld.run(seconds: Float, ax: Float, ay: Float) {
        repeat((seconds * 60).toInt()) { step(1 / 60f, ax, ay) }
    }

    private fun JarWorld.allInside() = bodies.all { it.x in 0f..width && it.y in 0f..height }

    @Test
    fun `pebbles never leave the jar, even when shaken hard`() {
        // Thin walls let pressed or fast pebbles slip out; a lost pebble also keeps the simulation awake forever.
        for (count in listOf(40, 400)) {
            val world = jar(count, size = if (count > 100) 28f else 80f, perRow = if (count > 100) 30 else 8)
            repeat(20) { i ->
                world.wake()
                val sign = if (i % 2 == 0) 1 else -1
                world.run(0.15f, sign * gravity * 3, -sign * gravity * 3)
            }
            world.run(1f, 0f, gravity)
            assertTrue(world.allInside(), "$count pebbles: some left the jar")
        }
    }

    @Test
    fun `a normal day's pile comes to a visible stop before the simulation is switched off`() {
        // Switching off a pile that still moves reads as a sudden freeze.
        val world = jar(40)
        world.run(3.3f, 0f, gravity)
        val before = world.bodies.map { it.x to it.y }
        world.run(0.1f, 0f, gravity)
        val fastest = world.bodies.zip(before).maxOf { (b, p) -> hypot(b.x - p.first, b.y - p.second) } * 10
        assertTrue(fastest < 5f, "fastest pebble $fastest px/s")
    }

    @Test
    fun `a very full jar still stops, so the screen does not redraw forever`() {
        val world = jar(500, size = 26f, perRow = 34)
        world.run(3.6f, 0f, gravity)
        assertTrue(world.resting)
    }

    @Test
    fun `tilting the phone to the right rolls the pile to the right`() {
        val world = jar(24)
        world.run(3f, 0f, gravity)
        val before = world.bodies.map { it.x }.average()
        world.wake()
        world.run(2f, gravity * 0.7f, gravity * 0.7f)
        val after = world.bodies.map { it.x }.average()
        assertTrue(after > before + 100, "centre moved from $before to $after")
    }

    @Test
    fun `settled pebbles keep their size instead of sinking into each other`() {
        val size = 80f
        val world = jar(40, size)
        world.run(3f, 0f, gravity)
        var closest = Float.MAX_VALUE
        for (i in world.bodies.indices) for (j in i + 1 until world.bodies.size) {
            val a = world.bodies[i]
            val b = world.bodies[j]
            closest = minOf(closest, hypot(b.x - a.x, b.y - a.y))
        }
        // Two triangles side by side, one upside down, touch with centres about 0.4 of the size apart.
        assertTrue(closest > size * 0.35f, "closest centres $closest")
    }

    @Test
    fun `a size change keeps each pebble at the same relative place`() {
        val world = jar(10)
        world.run(2f, 0f, gravity)
        val before = world.bodies.map { it.x / width to it.y / height }
        world.resize(width * 1.5f, height * 0.8f)
        val after = world.bodies.map { it.x / (width * 1.5f) to it.y / (height * 0.8f) }
        before.zip(after).forEach { (b, a) -> assertTrue(abs(b.first - a.first) < 0.001f && abs(b.second - a.second) < 0.001f) }
    }
}
