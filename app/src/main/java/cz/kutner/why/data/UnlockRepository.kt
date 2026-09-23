package cz.kutner.why.data

import cz.kutner.why.data.db.Reason
import cz.kutner.why.data.db.ReasonDao
import cz.kutner.why.data.db.TypedCount
import cz.kutner.why.data.db.UnlockDao
import cz.kutner.why.data.db.UnlockEvent
import kotlinx.coroutines.flow.Flow

class UnlockRepository(
    private val reasonDao: ReasonDao,
    private val unlockDao: UnlockDao,
) {
    val reasons: Flow<List<Reason>> = reasonDao.observeAll()

    suspend fun activeReasons(): List<Reason> = reasonDao.active()

    suspend fun seedIfEmpty(defaults: () -> List<Reason>) {
        if (reasonDao.count() == 0) reasonDao.insertAll(defaults())
    }

    suspend fun addReason(label: String, shape: String, color: String): Long =
        reasonDao.insert(Reason(label = label, shape = shape, color = color, position = reasonDao.nextPosition()))

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

    fun typedSuggestions(from: Long, minTimes: Int = 3): Flow<List<TypedCount>> = unlockDao.observeTyped(from, minTimes)
}
