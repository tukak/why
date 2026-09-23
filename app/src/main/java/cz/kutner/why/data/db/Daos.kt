package cz.kutner.why.data.db

import androidx.room3.Dao
import androidx.room3.Insert
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

    @Query(
        """
        SELECT MIN(TRIM(customText)) AS text, COUNT(*) AS times FROM unlock_event
        WHERE customText IS NOT NULL AND TRIM(customText) != '' AND unlockedAt >= :from
        GROUP BY LOWER(TRIM(customText)) HAVING COUNT(*) >= :minTimes
        ORDER BY times DESC
        """,
    )
    fun observeTyped(from: Long, minTimes: Int): Flow<List<TypedCount>>
}
