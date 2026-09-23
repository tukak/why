package cz.kutner.why.domain

import cz.kutner.why.data.db.CustomEntry
import cz.kutner.why.data.db.Reason
import cz.kutner.why.data.db.TypedOffer
import java.text.Normalizer
import java.util.Locale

/** Free-text answers grouped by meaning, so "Parkování", "parkovani " and "parkování." count as one. */
object TypedReasons {
    /** Offers to add a typed answer as a reason at 5, 10, 15… entries. */
    const val OFFER_EVERY = 5

    private val marks = Regex("\\p{Mn}+")
    private val spaces = Regex("\\s+")

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(marks, "")
            .lowercase(Locale.ROOT)
            .replace(spaces, " ")
            .trim()
            .trimEnd('.', '!', '?', ',', ';', ':')
            .trim()

    data class Group(val key: String, val label: String, val usedAt: List<Long>, val eventIds: List<Long>) {
        val count: Int get() = eventIds.size
    }

    /** The label is the spelling used most often; on a tie, the latest one. */
    fun group(entries: List<CustomEntry>): List<Group> =
        entries
            .filter { normalize(it.text).isNotEmpty() }
            .groupBy { normalize(it.text) }
            .map { (key, list) ->
                val spellings = list.groupBy { it.text.trim().replace(spaces, " ") }
                val label = spellings.maxWith(compareBy({ it.value.size }, { s -> s.value.maxOf { it.unlockedAt } })).key
                Group(key, label, list.map { it.unlockedAt }, list.map { it.id })
            }

    fun isDue(group: Group, offer: TypedOffer?): Boolean {
        if (offer?.never == true) return false
        val nextMilestone = ((offer?.lastOfferedCount ?: 0) / OFFER_EVERY + 1) * OFFER_EVERY
        return group.count >= nextMilestone
    }

    /** Groups that match an existing reason are not offered; the user already has that reason. */
    fun due(groups: List<Group>, offers: List<TypedOffer>, reasons: List<Reason>): List<Group> {
        val byKey = offers.associateBy { it.key }
        val existing = reasons.map { normalize(it.label) }.toSet()
        return groups.filter { it.key !in existing && isDue(it, byKey[it.key]) }.sortedByDescending { it.count }
    }

    /** Earlier answers to offer under the text field, in the same time-of-day order as the reasons. */
    fun suggestions(groups: List<Group>, reasons: List<Reason>, order: TimeOfDayOrder, limit: Int = 6): List<String> {
        val existing = reasons.map { normalize(it.label) }.toSet()
        val candidates = groups.filter { it.key !in existing }
        val rank = order.rank(candidates.flatMap { g -> g.usedAt.map { g.key to it } })
        return candidates
            .sortedWith(compareByDescending<Group> { rank.forNow[it.key] ?: 0 }.thenByDescending { rank.overall[it.key] ?: 0 })
            .take(limit)
            .map { it.label }
    }

    fun matches(suggestion: String, typed: String): Boolean {
        val query = normalize(typed)
        return query.isEmpty() || normalize(suggestion).contains(query)
    }
}
