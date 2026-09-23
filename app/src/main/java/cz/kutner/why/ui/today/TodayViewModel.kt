package cz.kutner.why.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.kutner.why.R
import cz.kutner.why.data.UnlockRepository
import cz.kutner.why.data.db.Reason
import cz.kutner.why.domain.Answer
import cz.kutner.why.domain.answer
import cz.kutner.why.domain.startOfDay
import cz.kutner.why.domain.summarizeDay
import cz.kutner.why.ui.components.UiText
import cz.kutner.why.ui.minuteTicker
import cz.kutner.why.ui.styleOf
import cz.kutner.why.ui.theme.PebbleStyle
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LegendItem(val label: UiText, val style: PebbleStyle, val count: Int)

data class TodayUiState(
    val date: LocalDate,
    val unlocks: Int,
    /** Positive: fewer unlocks than usual by now. Null without history. */
    val fewerThanUsual: Int?,
    val screenMillis: Long,
    val avgMillis: Long,
    val pebbles: List<PebbleStyle>,
    val legend: List<LegendItem>,
)

class TodayViewModel(unlocks: UnlockRepository, private val clock: Clock) : ViewModel() {

    private val weekAgo = startOfDay(clock.millis(), clock.zone) - 7 * DAY_MS

    val state: StateFlow<TodayUiState?> = combine(
        unlocks.reasons,
        unlocks.eventsSince(weekAgo),
        minuteTicker(),
    ) { reasons, events, _ ->
        val now = clock.millis()
        val dayStart = startOfDay(now, clock.zone)
        val (today, history) = events.partition { it.unlockedAt >= dayStart }
        val summary = summarizeDay(today, history, now, clock.zone)
        val byId = reasons.associateBy { it.id }
        TodayUiState(
            date = Instant.ofEpochMilli(now).atZone(clock.zone).toLocalDate(),
            unlocks = summary.unlocks,
            fewerThanUsual = summary.usualSoFar?.let { it - summary.unlocks },
            screenMillis = summary.screenMillis,
            avgMillis = summary.avgMillis,
            pebbles = today.map { styleOf(it.answer, byId) },
            legend = legend(today.map { it.answer }, byId),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun legend(answers: List<Answer>, byId: Map<Long, Reason>): List<LegendItem> =
        answers.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }
            .map { (answer, count) -> LegendItem(labelOf(answer, byId), styleOf(answer, byId), count) }

    private fun labelOf(answer: Answer, byId: Map<Long, Reason>): UiText = when (answer) {
        Answer.Habit -> UiText.Res(R.string.habit)
        Answer.Other -> UiText.Res(R.string.answer_other)
        Answer.None -> UiText.Res(R.string.answer_none)
        is Answer.Picked -> byId[answer.reasonId]?.let { UiText.Raw(it.label) } ?: UiText.Res(R.string.answer_other)
    }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
