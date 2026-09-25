package cz.kutner.why.data.db

import androidx.room3.ColumnInfo
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
    /** Whether a long session after this answer gets a check-in. */
    @ColumnInfo(defaultValue = "1") val nudge: Boolean = true,
)

/**
 * One screen session: from unlock to screen off.
 * Answer states: [reasonId] set, [isHabit] true, [customText] set, or [isAppCheck] true (unlocked to open this app).
 * All empty means unanswered.
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
    @ColumnInfo(defaultValue = "0") val isAppCheck: Boolean = false,
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
