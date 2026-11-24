package cz.kutner.why.domain

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TricksTest {
    private val all = Trick.entries.toSet()

    private fun pick(
        enabled: Set<Trick> = all,
        last: Trick? = null,
        answers: Int = 7,
        screenReader: Boolean = false,
        reducedMotion: Boolean = false,
        seed: Int = 1,
    ) = TrickPicker.pick(enabled, last, answers, screenReader, reducedMotion, Random(seed))

    @Test
    fun `the same trick never comes twice in a row`() {
        // A trick that repeats becomes a pattern the thumb learns, which is what the mode fights.
        repeat(200) { seed ->
            assertNotEquals(Trick.Pause, pick(enabled = setOf(Trick.Pause, Trick.Wild), last = Trick.Pause, seed = seed))
        }
    }

    @Test
    fun `a single enabled trick may repeat`() {
        assertEquals(Trick.Wild, pick(enabled = setOf(Trick.Wild), last = Trick.Wild))
    }

    @Test
    fun `only enabled tricks are picked`() {
        repeat(200) { seed -> assertTrue(pick(enabled = setOf(Trick.Hold, Trick.TypeIt), seed = seed) in setOf(Trick.Hold, Trick.TypeIt)) }
    }

    @Test
    fun `every trick can be picked`() {
        val seen = (0 until 2_000).mapNotNull { pick(seed = it) }.toSet()
        assertEquals(all, seen)
    }

    @Test
    fun `with animations off nothing that moves or animates is picked`() {
        // Moving answers can make people with motion sensitivity feel sick, and an animated trick ends at once with
        // animations off, so a hold or a breath would answer on touch.
        repeat(500) { seed ->
            val trick = pick(reducedMotion = true, seed = seed)!!
            assertTrue(!trick.moves && !trick.animated, "$trick")
        }
        assertEquals(TrickBlock.NoAnimations, Trick.Hold.block(answers = 7, screenReader = false, reducedMotion = true))
    }

    @Test
    fun `without a motion sensor tilt is never picked`() {
        // The ball cannot move, and tilt answers cannot be tapped, so the question could not be answered.
        repeat(500) { seed -> assertNotEquals(Trick.Tilt, TrickPicker.pick(all, null, 7, false, false, Random(seed), motionSensor = false)) }
        assertEquals(TrickBlock.NoSensor, Trick.Tilt.block(answers = 7, screenReader = false, reducedMotion = false, motionSensor = false))
    }

    @Test
    fun `with a screen reader no moving or drag trick is picked`() {
        // A screen reader user cannot aim at a moving target or drag by touch.
        repeat(500) { seed ->
            val trick = pick(screenReader = true, seed = seed)!!
            assertTrue(!trick.moves && !trick.drag)
        }
    }

    @Test
    fun `layouts with fixed places are skipped when the answers do not fit`() {
        repeat(500) { seed -> assertTrue(pick(answers = 18, seed = seed)!!.maxAnswers >= 18) }
        // The default 6 reasons and up to 16 fit every layout.
        assertEquals(Trick.entries.toSet(), (0 until 2_000).mapNotNull { pick(answers = 17, seed = it) }.toSet())
    }

    @Test
    fun `the block names the reason the settings screen shows`() {
        // Drag does not move by itself, so it still works with animations off; a screen reader cannot drag.
        assertNull(Trick.DragToJar.block(answers = 7, screenReader = false, reducedMotion = true))
        assertEquals(TrickBlock.ScreenReader, Trick.DragToJar.block(answers = 7, screenReader = true, reducedMotion = false))
        assertEquals(TrickBlock.NoAnimations, Trick.Drift.block(answers = 7, screenReader = false, reducedMotion = true))
        // Too many answers comes first: turning TalkBack off would not help.
        assertEquals(TrickBlock.TooManyAnswers, Trick.Tilt.block(answers = 18, screenReader = true, reducedMotion = true))
        // Tricks that neither move, animate, drag nor place every answer at once work for everyone.
        assertTrue(Trick.entries.filterNot { it.moves || it.animated || it.drag || it.maxAnswers != Int.MAX_VALUE }.all { it.block(answers = 99, screenReader = true, reducedMotion = true, motionSensor = false) == null })
    }

    @Test
    fun `nothing usable means no trick`() {
        assertNull(pick(enabled = setOf(Trick.Drift, Trick.Tilt), reducedMotion = true))
        assertNull(pick(enabled = emptySet()))
    }

    @Test
    fun `the habit trap puts Just habit where the first reason was`() {
        // The top-left place is where a habit tap lands; landing on "Just habit" records the truth.
        val setup = TrickSetup.create(Trick.HabitTrap, reasons = 6, Random(1))
        assertEquals(listOf(6, 1, 2, 3, 4, 5, 0), setup.order)
    }

    @Test
    fun `random order keeps every answer once and Just habit last`() {
        repeat(50) { seed ->
            val setup = TrickSetup.create(Trick.Shuffle, reasons = 6, Random(seed))
            assertEquals((0..6).toSet(), setup.order.toSet())
            assertEquals(6, setup.order.last())
        }
    }

    @Test
    fun `staggered wake-up wakes Just habit first`() {
        repeat(50) { seed ->
            val setup = TrickSetup.create(Trick.Stagger, reasons = 6, Random(seed))
            assertTrue(setup.wakeMs.getValue(setup.habit) < (0 until 6).minOf { setup.wakeMs.getValue(it) })
        }
    }

    @Test
    fun `the pause is 1 to 3 seconds`() {
        // A fixed length would let the thumb learn the timing.
        val pauses = (0 until 200).map { TrickSetup.create(Trick.Pause, 6, Random(it)).pauseMs }
        assertTrue(pauses.all { it in 1_000..3_000 })
        assertTrue(pauses.toSet().size > 50)
    }

    @Test
    fun `the circle grid is used only when the answers fit around it`() {
        repeat(200) { seed -> assertNotEquals(GridKind.Circle, TrickSetup.create(Trick.NewGrid, reasons = 7, Random(seed)).gridKind) }
    }

    @Test
    fun `scrambled labels keep spaces and become the label at the end`() {
        val label = "Look something up"
        val order = label.indices.toList().shuffled(Random(3))
        val source = label.indices.toList().shuffled(Random(4))
        val start = scrambled(label, order, source, 0f)
        assertEquals(label.indices.filter { label[it] == ' ' }, start.indices.filter { start[it] == ' ' })
        assertEquals(label.length, start.length)
        assertEquals(label, scrambled(label, order, source, 1f))
    }
}
