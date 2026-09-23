package cz.kutner.why.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReasonDao {
    @Query("SELECT * FROM reason ORDER BY position")
    fun observeAll(): Flow<List<Reason>>

    @Query("SELECT * FROM reason WHERE archived = 0 ORDER BY position")
    suspend fun active(): List<Reason>

    @Query("SELECT COUNT(*) FROM reason")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM reason")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insert(reason: Reason): Long

    @Insert
    suspend fun insertAll(reasons: List<Reason>)

    @Update
    suspend fun update(reasons: List<Reason>)
}

@Dao
interface UnlockDao {
    @Insert
    suspend fun insert(event: UnlockEvent): Long

    @Query("SELECT * FROM unlock_event WHERE id = :id")
    suspend fun get(id: Long): UnlockEvent?

    @Query("UPDATE unlock_event SET lockedAt = :at WHERE id = :id AND lockedAt IS NULL")
    suspend fun close(id: Long, at: Long)

    /** Sessions still open when the service starts ended while it was not running; their real end is unknown. */
    @Query("UPDATE unlock_event SET lockedAt = unlockedAt WHERE lockedAt IS NULL")
    suspend fun closeOrphans()

    @Query("UPDATE unlock_event SET lockedAt = NULL WHERE id = :id")
    suspend fun reopen(id: Long)

    @Query("UPDATE unlock_event SET reasonId = :reasonId, isHabit = :isHabit, customText = :customText WHERE id = :id")
    suspend fun answer(id: Long, reasonId: Long?, isHabit: Boolean, customText: String?)

    @Query("SELECT COUNT(*) FROM unlock_event WHERE unlockedAt >= :from")
    suspend fun countSince(from: Long): Int

    @Query("SELECT * FROM unlock_event WHERE unlockedAt >= :from ORDER BY unlockedAt")
    fun observeSince(from: Long): Flow<List<UnlockEvent>>

    @Query("SELECT id, customText AS text, unlockedAt FROM unlock_event WHERE customText IS NOT NULL")
    suspend fun customEntries(): List<CustomEntry>

    @Query("SELECT id, customText AS text, unlockedAt FROM unlock_event WHERE customText IS NOT NULL")
    fun observeCustomEntries(): Flow<List<CustomEntry>>

    @Query("UPDATE unlock_event SET reasonId = :reasonId, customText = NULL WHERE id IN (:ids)")
    suspend fun assignReason(ids: List<Long>, reasonId: Long)

    @Query("SELECT reasonId, unlockedAt FROM unlock_event WHERE reasonId IS NOT NULL AND unlockedAt >= :from")
    suspend fun reasonAnswers(from: Long): List<ReasonAnswer>
}

@Dao
interface OfferDao {
    @Query("SELECT * FROM typed_offer")
    suspend fun all(): List<TypedOffer>

    @Query("SELECT * FROM typed_offer")
    fun observeAll(): Flow<List<TypedOffer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(offer: TypedOffer)
}
