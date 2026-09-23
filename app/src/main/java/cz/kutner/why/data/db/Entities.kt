package cz.kutner.why.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "reason")
data class Reason(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    /** Key of [cz.kutner.why.ui.theme.PebbleShape]. */
    val shape: String,
    /** Key of [cz.kutner.why.ui.theme.ReasonColor]. */
    val color: String,
    val archived: Boolean = false,
)

/**
 * One screen session: from unlock to screen off.
 * Answer states: [reasonId] set, [isHabit] true, or [customText] set. All empty means unanswered.
 * [customKey] is [customText] normalized once on save, so grouping typed answers needs no text processing.
 */
@Entity(tableName = "unlock_event", indices = [Index("unlockedAt"), Index("customKey")])
data class UnlockEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unlockedAt: Long,
    val lockedAt: Long? = null,
    val reasonId: Long? = null,
    val isHabit: Boolean = false,
    val customText: String? = null,
    val customKey: String? = null,
)

/** What the user decided about adding a typed answer as a reason. [key] is the normalized text. */
@Entity(tableName = "typed_offer")
data class TypedOffer(
    @PrimaryKey val key: String,
    val lastOfferedCount: Int = 0,
    val never: Boolean = false,
)

data class CustomEntry(val id: Long, val text: String, val key: String, val unlockedAt: Long)

data class ReasonAnswer(val reasonId: Long, val unlockedAt: Long)
