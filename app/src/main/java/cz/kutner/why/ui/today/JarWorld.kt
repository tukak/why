package cz.kutner.why.ui.today

import cz.kutner.why.ui.theme.PebbleShape
import org.dyn4j.dynamics.Body
import org.dyn4j.dynamics.ContinuousDetectionMode
import org.dyn4j.geometry.Convex
import org.dyn4j.geometry.Geometry
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.World
import kotlin.math.cos
import kotlin.math.sin

/**
 * The jar's physics, on dyn4j. Triangle, pill, rounded square and diamond collide with their real outline;
 * the round-ish shapes (circle, clover, flower, burst) collide as circles.
 */
class JarWorld(
    private var width: Float,
    private var height: Float,
    private val cornerRadius: Float,
    private val inset: Float,
    /** dyn4j is tuned for objects of about 0.1–10 units; this keeps pebbles near 0.15. */
    private val pxPerUnit: Float = 100f,
) {
    class Pebble(var x: Float, var y: Float, var angle: Float)

    private val world = World<Body>().apply {
        settings.isAtRestDetectionEnabled = true
        // The defaults suit metre-sized objects; tiny pebbles in a deep pile creep just above them forever.
        settings.maximumAtRestLinearVelocity = 0.2
        settings.maximumAtRestAngularVelocity = 0.5
        settings.minimumAtRestTime = 0.5
        // A pebble moves at most 25 px per step and walls are 300 px thick, so none can jump through.
        settings.continuousDetectionMode = ContinuousDetectionMode.NONE
        settings.maximumTranslation = MAX_MOVE_PER_STEP / pxPerUnit.toDouble()
        // Fewer passes leave small overlaps, corrected every step: a restless pile, and no faster.
        settings.stepFrequency = 1.0 / 120.0
        settings.velocityConstraintSolverIterations = 10
        settings.positionConstraintSolverIterations = 10
        setGravity(0.0, 0.0)
    }
    private var walls = Body()
    private val pebbles = mutableListOf<Pair<Body, PebbleShape>>()
    /** Drawn pebble size in pixels; collision outlines follow the 24-unit drawing grid. */
    private var size = 1f
    /** Current position and angle of each pebble, in pixels from the jar's top left, in the order added. */
    val bodies = mutableListOf<Pebble>()

    /** Quiet by itself, or after the settle phase has damped every movement away. */
    val resting: Boolean get() = awakeSeconds > SETTLE_END || pebbles.all { it.first.isAtRest }
    private var awakeSeconds = 0f

    init {
        buildWalls()
    }

    fun add(x: Float, y: Float, size: Float, angle: Float, shape: PebbleShape) {
        this.size = size
        val body = Body()
        attach(body, shape)
        body.rotate(angle.toDouble())
        body.translate((x / pxPerUnit).toDouble(), (y / pxPerUnit).toDouble())
        body.setLinearDamping(BASE_DAMPING.toDouble())
        body.setAngularDamping(BASE_DAMPING.toDouble() * 2)
        world.addBody(body)
        pebbles += body to shape
        bodies += Pebble(x, y, angle)
    }

    fun removeLast() {
        val (body, _) = pebbles.removeAt(pebbles.lastIndex)
        world.removeBody(body)
        bodies.removeAt(bodies.lastIndex)
    }

    fun setSize(size: Float) {
        if (size == this.size) return
        this.size = size
        for ((body, shape) in pebbles) {
            body.removeAllFixtures()
            attach(body, shape)
        }
    }

    fun wake() {
        awakeSeconds = 0f
        appliedDamping = -1f
        pebbles.forEach { it.first.setAtRest(false) }
    }

    fun resize(newWidth: Float, newHeight: Float) {
        if (newWidth == width && newHeight == height) return
        val sx = newWidth / width
        val sy = newHeight / height
        for ((body, _) in pebbles) {
            val t = body.transform
            body.translate(t.translationX * (sx - 1), t.translationY * (sy - 1))
        }
        width = newWidth
        height = newHeight
        buildWalls()
        sync()
    }

    fun step(dt: Float, ax: Float, ay: Float) {
        world.setGravity((ax / pxPerUnit).toDouble(), (ay / pxPerUnit).toDouble())
        awakeSeconds += dt
        settle()
        world.update(dt.toDouble())
        sync()
    }

    private fun sync() {
        pebbles.forEachIndexed { i, (body, _) ->
            val t = body.transform
            bodies[i].x = (t.translationX * pxPerUnit).toFloat()
            bodies[i].y = (t.translationY * pxPerUnit).toFloat()
            bodies[i].angle = t.rotationAngle.toFloat()
        }
    }

    /**
     * After the tilt stays the same for a while, damping rises smoothly, so the pile comes to a natural stop
     * instead of being frozen mid-motion. A deep pile never gets fully quiet on its own.
     */
    private fun settle() {
        val progress = ((awakeSeconds - SETTLE_START) / (SETTLE_END - SETTLE_START)).coerceIn(0f, 1f)
        val damping = BASE_DAMPING + SETTLE_DAMPING * progress * progress
        if (damping == appliedDamping) return
        appliedDamping = damping
        for ((body, _) in pebbles) {
            body.setLinearDamping(damping.toDouble())
            body.setAngularDamping(damping.toDouble() * 2)
        }
    }

    private var appliedDamping = -1f

    private fun attach(body: Body, shape: PebbleShape) {
        body.addFixture(convex(shape, size / pxPerUnit), 1.0, PEBBLE_FRICTION, 0.0)
        body.setMass(MassType.NORMAL)
    }

    /** Outlines from the 24-unit drawing grid, centred on the drawing's centre so rendering stays aligned. */
    private fun convex(shape: PebbleShape, size: Float): Convex {
        val k = size / 24.0
        fun v(x: Double, y: Double) = Vector2((x - 12) * k, (y - 12) * k)
        return when (shape) {
            PebbleShape.Pill -> Geometry.createCapsule(20 * k, 12 * k)
            PebbleShape.Triangle -> Geometry.createTriangle(v(12.0, 3.0), v(21.5, 19.5), v(2.5, 19.5))
            PebbleShape.Squircle -> Geometry.createSquare(20 * k)
            PebbleShape.Diamond -> Geometry.createPolygon(v(12.0, 2.0), v(22.0, 12.0), v(12.0, 22.0), v(2.0, 12.0))
            else -> Geometry.createCircle(11.5 * k)
        }
    }

    /**
     * Solid walls with no gaps: four wide blocks outside the sides, and for each rounded corner a fan of
     * triangles from the arc to the box corner. Thin segments or strips let pressed pebbles slip out.
     */
    private fun buildWalls() {
        world.removeBody(walls)
        walls = Body()
        val r = (cornerRadius - inset).toDouble() / pxPerUnit
        val left = inset.toDouble() / pxPerUnit
        val top = inset.toDouble() / pxPerUnit
        val right = (width - inset).toDouble() / pxPerUnit
        val bottom = (height - inset).toDouble() / pxPerUnit
        val t = WALL_THICKNESS / pxPerUnit.toDouble()
        solid(Vector2(left - t, top - t), Vector2(left, top - t), Vector2(left, bottom + t), Vector2(left - t, bottom + t))
        solid(Vector2(right, top - t), Vector2(right + t, top - t), Vector2(right + t, bottom + t), Vector2(right, bottom + t))
        solid(Vector2(left, top - t), Vector2(right, top - t), Vector2(right, top), Vector2(left, top))
        solid(Vector2(left, bottom), Vector2(right, bottom), Vector2(right, bottom + t), Vector2(left, bottom + t))
        val corners = listOf(
            Triple(Vector2(right - r, bottom - r), Vector2(right, bottom), 0.0),
            Triple(Vector2(left + r, bottom - r), Vector2(left, bottom), Math.PI / 2),
            Triple(Vector2(left + r, top + r), Vector2(left, top), Math.PI),
            Triple(Vector2(right - r, top + r), Vector2(right, top), 3 * Math.PI / 2),
        )
        for ((centre, boxCorner, start) in corners) {
            val arc = (0..CORNER_SEGMENTS).map { s ->
                val a = start + s * (Math.PI / 2) / CORNER_SEGMENTS
                Vector2(centre.x + r * cos(a), centre.y + r * sin(a))
            }
            for (i in 0 until arc.size - 1) solid(arc[i], arc[i + 1], boxCorner)
        }
        walls.setMass(MassType.INFINITE)
        world.addBody(walls)
    }

    /** Adds a convex wall piece; dyn4j wants the points counter-clockwise, so the order is fixed here. */
    private fun solid(vararg points: Vector2) {
        val area2 = (1 until points.size - 1).sumOf { k ->
            (points[k].x - points[0].x) * (points[k + 1].y - points[0].y) - (points[k + 1].x - points[0].x) * (points[k].y - points[0].y)
        }
        if (kotlin.math.abs(area2) < 1e-9) return
        walls.addFixture(Geometry.createPolygon(*(if (area2 > 0) points else points.reversedArray())), 1.0, 0.5, 0.1)
    }

    private companion object {
        const val CORNER_SEGMENTS = 6
        const val WALL_THICKNESS = 300f
        const val MAX_MOVE_PER_STEP = 25f
        const val BASE_DAMPING = 0.05f
        const val PEBBLE_FRICTION = 0.5
        const val SETTLE_DAMPING = 12f
        const val SETTLE_START = 1.5f
        const val SETTLE_END = 3.5f
    }
}
