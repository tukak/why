package cz.kutner.why.ui.overlay

import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.kutner.why.R
import cz.kutner.why.data.db.Reason
import cz.kutner.why.domain.Trick
import cz.kutner.why.domain.TrickSetup

sealed interface PromptAnswer {
    data class Picked(val reason: Reason) : PromptAnswer
    data object Habit : PromptAnswer
    data class Typed(val text: String) : PromptAnswer
}

/**
 * [answerMs] counts from when the answer woke up, or, for an answer that never slept, from when the question was created,
 * which is before the unlock animation ends. [earlyTaps] are taps before the answer woke up.
 */
data class AnswerStats(val answerMs: Long, val earlyTaps: Int)

/** When each answer wakes up, and what the taps on a question screen did so far. */
@Stable
internal class PromptState(val setup: TrickSetup?, private val clock: () -> Long = SystemClock::uptimeMillis) {
    val trick: Trick? = setup?.trick
    private val shownAt = clock()

    /** When the answers (or, with [Trick.Stagger], the text field) became tappable; null while they sleep. */
    var openAt by mutableStateOf(if (trick in GATED) null else shownAt)
        private set
    private val wokeAt = mutableStateMapOf<Int, Long>()
    var earlyTaps by mutableIntStateOf(0)
        private set
    var pauseRound by mutableIntStateOf(0)
        private set

    @get:StringRes
    var message by mutableStateOf<Int?>(null)
    var pending by mutableStateOf<Pending?>(null)
    var typed by mutableStateOf("")

    fun awake(index: Int): Boolean = if (trick == Trick.Stagger && index != TYPED) index in wokeAt else openAt != null

    fun open() {
        openAt = clock()
        message = null
    }

    fun wake(index: Int) {
        wokeAt[index] = clock()
    }

    /** The answer to deliver now, or null when the tap was early or waits for the second check. */
    fun tap(index: Int, answer: PromptAnswer, label: String): Pair<PromptAnswer, AnswerStats>? {
        if (!awake(index)) {
            earlyTaps++
            if (trick == Trick.EarlyReset) {
                message = R.string.prompt_too_fast
                openAt = null
                pauseRound++
            }
            return null
        }
        val stats = AnswerStats(clock() - (wokeAt[index] ?: openAt ?: shownAt), earlyTaps)
        if (trick == Trick.SecondCheck) {
            pending = Pending(answer, label, stats)
            return null
        }
        return answer to stats
    }

    class Pending(val answer: PromptAnswer, val label: String, val stats: AnswerStats)

    companion object {
        /** The index of a typed answer. */
        const val TYPED = -1
        private val GATED = setOf(Trick.Pause, Trick.EarlyReset, Trick.Scramble, Trick.Breath)
    }
}
