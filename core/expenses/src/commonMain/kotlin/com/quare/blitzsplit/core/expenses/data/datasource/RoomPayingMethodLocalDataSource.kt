package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.PayingMethodDao
import com.quare.blitzsplit.core.provider.room.entity.BillPayingMethodEntity
import com.quare.blitzsplit.core.provider.room.entity.PayingMethodEntity
import kotlinx.coroutines.flow.Flow

class RoomPayingMethodLocalDataSource(
    private val dao: PayingMethodDao,
) : PayingMethodLocalDataSource {
    override fun observeLiveMethods(): Flow<List<PayingMethodEntity>> = dao.observeLiveMethods()

    override fun observeMarks(): Flow<List<BillPayingMethodEntity>> = dao.observeMarks()

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override suspend fun getAllMethods(): List<PayingMethodEntity> = dao.getAllMethods()

    override suspend fun getAllMarks(): List<BillPayingMethodEntity> = dao.getAllMarks()

    override suspend fun getMarksReadyToUpload(): List<BillPayingMethodEntity> = dao.getMarksReadyToUpload()

    override suspend fun upsertMethods(entities: List<PayingMethodEntity>) {
        dao.upsertMethods(entities)
    }

    override suspend fun upsertMarks(entities: List<BillPayingMethodEntity>) {
        dao.upsertMarks(entities)
    }

    override suspend fun markMethodSynced(
        id: String,
        syncedUpdatedAt: Long,
    ) {
        dao.markMethodSynced(id, syncedUpdatedAt)
    }

    override suspend fun markMarkSynced(
        expenseId: String,
        syncedUpdatedAt: Long,
    ) {
        dao.markMarkSynced(expenseId, syncedUpdatedAt)
    }

    override suspend fun deleteSyncedTombstones() {
        dao.deleteSyncedTombstones()
    }

    override suspend fun deleteOrphanMarks() {
        dao.deleteOrphanMarks()
    }

    override suspend fun keepOnlyUser(userId: String) {
        dao.deleteOtherUsersMethods(userId)
        dao.deleteOtherUsersMarks(userId)
        dao.claimUnownedMethods(userId)
        dao.claimUnownedMarks(userId)
    }

    override suspend fun clear() {
        dao.deleteAllMarks()
        dao.deleteAllMethods()
    }
}
