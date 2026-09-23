package cz.kutner.why.ui.reasons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.kutner.why.data.UnlockRepository
import cz.kutner.why.data.db.Reason
import cz.kutner.why.domain.startOfDay
import cz.kutner.why.ui.theme.PebbleShape
import cz.kutner.why.ui.theme.ReasonColor
import java.time.Clock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReasonRow(val reason: Reason, val count: Int)

data class ReasonsUiState(
    val active: List<ReasonRow> = emptyList(),
    val archived: List<Reason> = emptyList(),
    val suggestion: String? = null,
)

class ReasonsViewModel(private val unlocks: UnlockRepository, clock: Clock) : ViewModel() {
    private val monthAgo = startOfDay(clock.millis(), clock.zone) - 30 * DAY_MS
    private val weekAgo = startOfDay(clock.millis(), clock.zone) - 7 * DAY_MS

    val state: StateFlow<ReasonsUiState> = combine(
        unlocks.reasons,
        unlocks.eventsSince(monthAgo),
        unlocks.typedSuggestions(weekAgo),
    ) { reasons, events, typed ->
        val counts = events.mapNotNull { it.reasonId }.groupingBy { it }.eachCount()
        val labels = reasons.map { it.label.lowercase() }.toSet()
        ReasonsUiState(
            active = reasons.filterNot { it.archived }.map { ReasonRow(it, counts[it.id] ?: 0) },
            archived = reasons.filter { it.archived },
            suggestion = typed.firstOrNull { it.text.lowercase() !in labels }?.text,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReasonsUiState())

    fun add(label: String, shape: PebbleShape? = null, color: ReasonColor? = null) = viewModelScope.launch {
        val used = state.value.active.map { it.reason }
        val nextShape = shape ?: PebbleShape.pickable.minBy { s -> used.count { it.shape == s.name } }
        val nextColor = color ?: ReasonColor.pickable.minBy { c -> used.count { it.color == c.name } }
        unlocks.addReason(label.trim(), nextShape.name, nextColor.name)
    }

    fun save(reason: Reason) = viewModelScope.launch { unlocks.updateReasons(listOf(reason.copy(label = reason.label.trim()))) }

    fun setArchived(reason: Reason, archived: Boolean) = viewModelScope.launch {
        unlocks.updateReasons(listOf(reason.copy(archived = archived)))
    }

    /** Moves a reason one place up (-1) or down (+1) among active reasons. */
    fun move(reason: Reason, by: Int) = viewModelScope.launch {
        val list = state.value.active.map { it.reason }.toMutableList()
        val from = list.indexOfFirst { it.id == reason.id }
        val to = from + by
        if (from < 0 || to !in list.indices) return@launch
        list.add(to, list.removeAt(from))
        unlocks.updateReasons(list.mapIndexed { i, r -> r.copy(position = i) })
    }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
