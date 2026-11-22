package cz.kutner.why.domain

import kotlin.random.Random

enum class TrickGroup { Timing, Layout, Words, Body }

/** Layouts that place every answer on screen at once get too small above this many answers on a small phone. */
const val MAX_PLACED = 17

/**
 * One way annoying mode changes the question screen, so a tap from habit does not land.
 * The names are stored; do not rename them.
 * [moves]: the answers move or roll. [drag]: answering needs a drag. [maxAnswers]: more answers do not fit the layout.
 */
enum class Trick(val group: TrickGroup, val moves: Boolean = false, val drag: Boolean = false, val maxAnswers: Int = Int.MAX_VALUE) {
    Pause(TrickGroup.Timing),
    Stagger(TrickGroup.Timing),
    EarlyReset(TrickGroup.Timing),
    Hold(TrickGroup.Timing),
    SecondCheck(TrickGroup.Timing),
    Shuffle(TrickGroup.Layout),
    HabitTrap(TrickGroup.Layout),
    NewGrid(TrickGroup.Layout),
    Wild(TrickGroup.Layout),
    Drift(TrickGroup.Layout, moves = true, maxAnswers = MAX_PLACED),
    Pebbles(TrickGroup.Layout, moves = true, maxAnswers = MAX_PLACED),
    Scramble(TrickGroup.Words),
    TypeIt(TrickGroup.Words),
    Question(TrickGroup.Words),
    Tilt(TrickGroup.Body, moves = true, maxAnswers = MAX_PLACED),
    Breath(TrickGroup.Body),
    DragToJar(TrickGroup.Body, drag = true, maxAnswers = MAX_PLACED),
    ;

    companion object {
        fun of(key: String): Trick? = entries.firstOrNull { it.name == key }
    }
}

object TrickPicker {
    /**
     * A random trick from [enabled] that is not [last], or null when none can be used.
     * With a screen reader, moving targets and drags cannot be used; with animations off, nothing may move.
     */
    fun pick(enabled: Set<Trick>, last: Trick?, answers: Int, screenReader: Boolean, reducedMotion: Boolean, random: Random): Trick? {
        val usable = enabled.filter { t ->
            answers <= t.maxAnswers && !(t.moves && (screenReader || reducedMotion)) && !(t.drag && screenReader)
        }
        if (usable.isEmpty()) return null
        return usable.filter { it != last }.ifEmpty { usable }.random(random)
    }
}

enum class GridKind { ThreeColumns, List, Circle }

/**
 * The random choices for one question screen, made once so the screen does not change while it shows.
 * Answers are numbered: reasons 0 until [reasons], then "Just habit" as [habit].
 * [order] is the display order; layouts with a wide button at the bottom put the last entry there.
 */
data class TrickSetup(
    val trick: Trick,
    val reasons: Int,
    val order: List<Int>,
    val pauseMs: Long,
    val wakeMs: Map<Int, Long>,
    val gridKind: GridKind,
    val question: Int,
    val seed: Long,
) {
    val habit: Int get() = reasons

    companion object {
        const val QUESTIONS = 4
        const val SCRAMBLE_MS = 2_400L
        const val CIRCLE_MAX_ANSWERS = 7

        fun create(trick: Trick, reasons: Int, random: Random): TrickSetup {
            val ids = (0 until reasons).toList()
            val habit = reasons
            val order = when (trick) {
                Trick.Shuffle -> ids.shuffled(random) + habit
                // The top-left place is where a habit tap lands; the reason that sat there takes the wide button.
                Trick.HabitTrap -> if (reasons == 0) listOf(habit) else listOf(habit) + ids.drop(1) + ids.first()
                else -> ids + habit
            }
            val wake = if (trick == Trick.Stagger) {
                // "Just habit" wakes first: the honest answer is the quickest one.
                ids.associateWith { random.nextLong(700, 3_200) } + (habit to 300L)
            } else {
                emptyMap()
            }
            val kinds = GridKind.entries.filter { it != GridKind.Circle || reasons + 1 <= CIRCLE_MAX_ANSWERS }
            return TrickSetup(
                trick = trick,
                reasons = reasons,
                order = order,
                pauseMs = random.nextLong(1_000, 3_001),
                wakeMs = wake,
                gridKind = kinds.random(random),
                question = random.nextInt(QUESTIONS),
                seed = random.nextLong(),
            )
        }
    }
}

/**
 * [label] with its letters mixed, more of them in place as [progress] goes from 0 to 1. Spaces stay, so the word
 * lengths show from the start. [order] is the order letters land in; [source] picks a wrong letter for each place.
 */
fun scrambled(label: String, order: List<Int>, source: List<Int>, progress: Float): String {
    val placed = order.take((progress.coerceIn(0f, 1f) * label.length).toInt()).toSet()
    return buildString {
        label.forEachIndexed { i, c ->
            when {
                c == ' ' || i in placed -> append(c)
                else -> append(label[source[i]].takeIf { it != ' ' }?.lowercaseChar() ?: 'o')
            }
        }
    }
}
