package cz.kutner.why.ui.today

import kotlin.math.hypot
import kotlin.math.sqrt

/** Round bodies inside a rounded rectangle. Units are pixels and seconds. */
class PebbleWorld(
    private val width: Float,
    private val height: Float,
    private val cornerRadius: Float,
    private val inset: Float,
    /** Below this speed for a while, the world counts as resting and stops simulating. */
    private val restSpeed: Float,
) {
    class Body(var x: Float, var y: Float, var radius: Float, var angle: Float = 0f) {
        var vx = 0f
        var vy = 0f
        var spin = 0f
        internal var startX = x
        internal var startY = y
        internal var wallX = 0f
        internal var wallY = 0f
    }

    val bodies = mutableListOf<Body>()

    var resting = false
        private set
    private var quietSteps = 0

    fun wake() {
        quietSteps = 0
        resting = false
    }

    fun step(dt: Float, ax: Float, ay: Float) {
        val h = dt / SUBSTEPS
        repeat(SUBSTEPS) { substep(h, ax, ay) }
        val fastest = bodies.maxOfOrNull { hypot(it.vx, it.vy) } ?: 0f
        quietSteps = if (fastest < restSpeed) quietSteps + 1 else 0
        resting = quietSteps > REST_STEPS
    }

    /** Position-based: velocity comes from the real movement, so a pebble held by others really stops. */
    private fun substep(h: Float, ax: Float, ay: Float) {
        for (b in bodies) {
            b.startX = b.x
            b.startY = b.y
            b.wallX = 0f
            b.wallY = 0f
            b.vx += ax * h
            b.vy += ay * h
            b.x += b.vx * h
            b.y += b.vy * h
        }
        repeat(ITERATIONS) {
            for (i in bodies.indices) for (j in i + 1 until bodies.size) collide(bodies[i], bodies[j])
            for (b in bodies) constrain(b)
        }
        for (b in bodies) {
            val incoming = b.vx * b.wallX + b.vy * b.wallY
            b.vx = (b.x - b.startX) / h * DAMPING
            b.vy = (b.y - b.startY) / h * DAMPING
            if (b.wallX != 0f || b.wallY != 0f) wallContact(b, incoming)
            b.angle += b.spin * h
            b.spin *= SPIN_DAMPING
        }
    }

    private fun collide(a: Body, b: Body) {
        var dx = b.x - a.x
        var dy = b.y - a.y
        val min = a.radius + b.radius
        var d2 = dx * dx + dy * dy
        if (d2 >= min * min) return
        if (d2 == 0f) {
            dx = 0.01f
            d2 = dx * dx
        }
        val d = sqrt(d2)
        val nx = dx / d
        val ny = dy / d
        val push = (min - d) * 0.5f
        a.x -= nx * push
        a.y -= ny * push
        b.x += nx * push
        b.y += ny * push
        val vt = (b.vx - a.vx) * -ny + (b.vy - a.vy) * nx
        a.spin += vt / a.radius * SPIN_TRANSFER
        b.spin -= vt / b.radius * SPIN_TRANSFER
    }

    private fun constrain(b: Body) {
        val cx = when {
            b.x < cornerRadius -> cornerRadius
            b.x > width - cornerRadius -> width - cornerRadius
            else -> null
        }
        val cy = when {
            b.y < cornerRadius -> cornerRadius
            b.y > height - cornerRadius -> height - cornerRadius
            else -> null
        }
        val limit = cornerRadius - inset - b.radius
        if (cx != null && cy != null && limit > 0) {
            val dx = b.x - cx
            val dy = b.y - cy
            val d = hypot(dx, dy)
            if (d > limit) {
                b.x = cx + dx / d * limit
                b.y = cy + dy / d * limit
                touchWall(b, dx / d, dy / d)
            }
        }
        val minEdge = inset + b.radius
        if (b.x < minEdge) { b.x = minEdge; touchWall(b, -1f, 0f) }
        if (b.x > width - minEdge) { b.x = width - minEdge; touchWall(b, 1f, 0f) }
        if (b.y < minEdge) { b.y = minEdge; touchWall(b, 0f, -1f) }
        if (b.y > height - minEdge) { b.y = height - minEdge; touchWall(b, 0f, 1f) }
    }

    /** [nx], [ny]: the wall normal pointing out of the jar. */
    private fun touchWall(b: Body, nx: Float, ny: Float) {
        b.wallX = nx
        b.wallY = ny
    }

    /** Hard hits bounce back; everything else rolls along the wall with a little friction. */
    private fun wallContact(b: Body, incoming: Float) {
        val nx = b.wallX
        val ny = b.wallY
        if (incoming > BOUNCE_SPEED * restSpeed) {
            val vn = b.vx * nx + b.vy * ny
            b.vx -= (vn + WALL_RESTITUTION * incoming) * nx
            b.vy -= (vn + WALL_RESTITUTION * incoming) * ny
        }
        val vt = b.vx * -ny + b.vy * nx
        b.vx -= vt * -ny * WALL_FRICTION
        b.vy -= vt * nx * WALL_FRICTION
        b.spin = vt / b.radius
    }

    private companion object {
        const val SUBSTEPS = 4
        const val ITERATIONS = 2
        const val DAMPING = 0.9995f
        const val SPIN_DAMPING = 0.985f
        const val SPIN_TRANSFER = 0.02f
        const val WALL_RESTITUTION = 0.35f
        /** In multiples of the rest speed. */
        const val BOUNCE_SPEED = 10f
        const val WALL_FRICTION = 0.015f
        const val REST_STEPS = 30
    }
}
