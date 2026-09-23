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
    val position: Int,
    val archived: Boolean = false,
)

/**
 * One screen session: from unlock to screen off.
 * Answer states: [reasonId] set, [isHabit] true, or [customText] set. All empty means unanswered.
 */
@Entity(tableName = "unlock_event", indices = [Index("unlockedAt")])
data class UnlockEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unlockedAt: Long,
    val lockedAt: Long? = null,
    val reasonId: Long? = null,
    val isHabit: Boolean = false,
    val customText: String? = null,
)

data class TypedCount(val text: String, val times: Int)
