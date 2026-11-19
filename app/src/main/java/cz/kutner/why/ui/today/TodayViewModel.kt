package cz.kutner.why.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.kutner.why.data.UnlockRepository
import cz.kutner.why.data.db.Reason
import cz.kutner.why.domain.Answer
import cz.kutner.why.domain.DaySummary
import cz.kutner.why.domain.USUAL_DAYS
import cz.kutner.why.domain.answer
import cz.kutner.why.domain.daysBack
import cz.kutner.why.domain.startOfDay
import cz.kutner.why.domain.summarizeDay
import cz.kutner.why.ui.components.UiText
import cz.kutner.why.ui.labelOf
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
    val summary: DaySummary,
    val pebbles: List<PebbleStyle>,
    val legend: List<LegendItem>,
)

class TodayViewModel(unlocks: UnlockRepository, private val clock: Clock) : ViewModel() {

    private val historyStart = daysBack(clock.millis(), clock.zone, USUAL_DAYS)

    val state: StateFlow<TodayUiState?> = combine(
        unlocks.reasons,
        unlocks.eventsSince(historyStart),
        minuteTicker(),
    ) { reasons, events, _ ->
        val now = clock.millis()
        val dayStart = startOfDay(now, clock.zone)
        val (today, history) = events.partition { it.unlockedAt >= dayStart }
        val byId = reasons.associateBy { it.id }
        TodayUiState(
            date = Instant.ofEpochMilli(now).atZone(clock.zone).toLocalDate(),
            summary = summarizeDay(today, history, now, clock.zone),
            pebbles = today.map { styleOf(it.answer, byId) },
            legend = legend(today.map { it.answer }, byId),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun legend(answers: List<Answer>, byId: Map<Long, Reason>): List<LegendItem> =
        answers.groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }
            .map { (answer, count) -> LegendItem(labelOf(answer, byId), styleOf(answer, byId), count) }
}
