package cz.kutner.why.ui.reasons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.kutner.why.data.OfferDecision
import cz.kutner.why.data.UnlockRepository
import cz.kutner.why.data.db.Reason
import cz.kutner.why.domain.TypedReasons
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
    val offer: TypedReasons.Group? = null,
)

class ReasonsViewModel(private val unlocks: UnlockRepository, clock: Clock) : ViewModel() {
    private val monthAgo = startOfDay(clock.millis(), clock.zone) - 30 * DAY_MS

    val state: StateFlow<ReasonsUiState> = combine(
        unlocks.reasons,
        unlocks.eventsSince(monthAgo),
        unlocks.dueOffers,
    ) { reasons, events, offers ->
        val counts = events.mapNotNull { it.reasonId }.groupingBy { it }.eachCount()
        ReasonsUiState(
            active = reasons.filterNot { it.archived }
                .map { ReasonRow(it, counts[it.id] ?: 0) }
                .sortedWith(compareByDescending<ReasonRow> { it.count }.thenBy { it.reason.position }),
            archived = reasons.filter { it.archived },
            offer = offers.firstOrNull(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReasonsUiState())

    fun add(label: String, shape: PebbleShape? = null, color: ReasonColor? = null) = viewModelScope.launch {
        unlocks.addReason(label, shape, color)
    }

    fun save(reason: Reason) = viewModelScope.launch { unlocks.updateReasons(listOf(reason.copy(label = reason.label.trim()))) }

    fun setArchived(reason: Reason, archived: Boolean) = viewModelScope.launch {
        unlocks.updateReasons(listOf(reason.copy(archived = archived)))
    }

    fun decide(offer: TypedReasons.Group, decision: OfferDecision) = viewModelScope.launch { unlocks.decide(offer, decision) }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
