package cz.kutner.why.data

import cz.kutner.why.data.db.OfferDao
import cz.kutner.why.data.db.Reason
import cz.kutner.why.data.db.ReasonDao
import cz.kutner.why.data.db.TypedOffer
import cz.kutner.why.data.db.UnlockDao
import cz.kutner.why.data.db.UnlockEvent
import cz.kutner.why.domain.TimeOfDayOrder
import cz.kutner.why.domain.TypedReasons
import cz.kutner.why.ui.theme.PebbleShape
import cz.kutner.why.ui.theme.ReasonColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

enum class OfferDecision { Add, NotNow, Never }

/** What the question window shows: reasons and earlier typed answers, both ordered for this time of day. */
data class PromptChoices(val reasons: List<Reason>, val typed: List<String>)

class UnlockRepository(
    private val reasonDao: ReasonDao,
    private val unlockDao: UnlockDao,
    private val offerDao: OfferDao,
) {
    val reasons: Flow<List<Reason>> = reasonDao.observeAll()

    /** Typed answers that are due to be offered as a reason, most typed first. */
    val dueOffers: Flow<List<TypedReasons.Group>> =
        combine(unlockDao.observeCustomEntries(), offerDao.observeAll(), reasonDao.observeAll()) { entries, offers, reasons ->
            TypedReasons.due(TypedReasons.group(entries), offers, reasons)
        }

    suspend fun promptChoices(order: TimeOfDayOrder, answersSince: Long): PromptChoices {
        val active = reasonDao.active()
        val answers = unlockDao.reasonAnswers(answersSince).map { it.reasonId to it.unlockedAt }
        val typed = TypedReasons.suggestions(TypedReasons.group(unlockDao.customEntries()), active, order)
        val ordered = order.order(active, answers)
        return PromptChoices(ordered, typed)
    }

    suspend fun dueOfferFor(text: String): TypedReasons.Group? {
        val key = TypedReasons.normalize(text)
        return TypedReasons.due(TypedReasons.group(unlockDao.customEntries()), offerDao.all(), reasonDao.active())
            .firstOrNull { it.key == key }
    }

    /** Adding also moves the earlier typed answers to the new reason, so its stats start complete. */
    suspend fun decide(offer: TypedReasons.Group, decision: OfferDecision) {
        when (decision) {
            OfferDecision.Add -> {
                val id = addReason(offer.label)
                unlockDao.assignReason(offer.eventIds, id)
                offerDao.save(TypedOffer(offer.key, offer.count, never = true))
            }
            OfferDecision.NotNow -> offerDao.save(TypedOffer(offer.key, offer.count))
            OfferDecision.Never -> offerDao.save(TypedOffer(offer.key, offer.count, never = true))
        }
    }

    suspend fun seedIfEmpty(defaults: () -> List<Reason>) {
        if (reasonDao.count() == 0) reasonDao.insertAll(defaults())
    }

    /** Without a chosen style, picks the shape and color used least among active reasons. */
    suspend fun addReason(label: String, shape: PebbleShape? = null, color: ReasonColor? = null): Long {
        val active = reasonDao.active()
        val pickedShape = shape ?: PebbleShape.pickable.minBy { s -> active.count { it.shape == s.name } }
        val pickedColor = color ?: ReasonColor.pickable.minBy { c -> active.count { it.color == c.name } }
        return reasonDao.insert(
            Reason(label = label.trim(), shape = pickedShape.name, color = pickedColor.name, position = reasonDao.nextPosition()),
        )
    }

    suspend fun updateReasons(reasons: List<Reason>) = reasonDao.update(reasons)

    suspend fun startSession(at: Long): Long = unlockDao.insert(UnlockEvent(unlockedAt = at))

    suspend fun event(id: Long): UnlockEvent? = unlockDao.get(id)

    suspend fun resumeSession(id: Long) = unlockDao.reopen(id)

    suspend fun closeSession(id: Long, at: Long) = unlockDao.close(id, at)

    suspend fun closeOrphanSessions() = unlockDao.closeOrphans()

    suspend fun answer(id: Long, reasonId: Long? = null, isHabit: Boolean = false, customText: String? = null) =
        unlockDao.answer(id, reasonId, isHabit, customText?.trim()?.takeIf { it.isNotEmpty() })

    suspend fun countSince(from: Long): Int = unlockDao.countSince(from)

    fun eventsSince(from: Long): Flow<List<UnlockEvent>> = unlockDao.observeSince(from)
}
