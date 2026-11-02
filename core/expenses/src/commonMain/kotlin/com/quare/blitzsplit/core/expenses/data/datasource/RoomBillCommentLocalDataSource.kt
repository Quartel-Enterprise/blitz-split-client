package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.BillCommentDao
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
import kotlinx.coroutines.flow.Flow

class RoomBillCommentLocalDataSource(
    private val dao: BillCommentDao,
) : BillCommentLocalDataSource {
    override fun observeComments(expenseId: String): Flow<List<BillCommentEntity>> = dao.observeComments(expenseId)

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override fun observePendingIds(): Flow<List<String>> = dao.observePendingIds()

    override suspend fun getPendingComments(ownerUserId: String): List<BillCommentEntity> =
        dao.getPendingComments(ownerUserId)

    override suspend fun insertComment(entity: BillCommentEntity) {
        dao.insertCommentOnBill(entity)
    }

    override suspend fun applyRemote(entities: List<BillCommentEntity>) {
        dao.applyRemoteToBills(entities)
    }

    override suspend fun markSynced(ids: List<String>) {
        dao.markSynced(ids)
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverCommentIds: Set<String>,
    ) {
        dao.deleteGoneFromServer(
            ownerUserId = ownerUserId,
            serverCommentIds = serverCommentIds,
        )
    }
}
