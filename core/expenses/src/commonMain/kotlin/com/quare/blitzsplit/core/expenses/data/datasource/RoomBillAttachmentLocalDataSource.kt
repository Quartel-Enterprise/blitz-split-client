package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.BillAttachmentDao
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity
import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentRow
import kotlinx.coroutines.flow.Flow

class RoomBillAttachmentLocalDataSource(
    private val dao: BillAttachmentDao,
) : BillAttachmentLocalDataSource {
    override fun observeAttachments(expenseId: String): Flow<List<BillAttachmentRow>> =
        dao.observeAttachments(expenseId)

    override suspend fun getPendingJpeg(id: String): ByteArray? = dao.getPendingJpeg(id)

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override fun observePendingVersions(): Flow<List<String>> = dao.observePendingVersions()

    override suspend fun getPendingAttachmentIds(ownerUserId: String): List<String> =
        dao.getPendingAttachmentIds(ownerUserId)

    override suspend fun getAttachment(id: String): BillAttachmentEntity? = dao.getAttachment(id)

    override suspend fun insertWithinLimit(
        entity: BillAttachmentEntity,
        limit: Int,
    ): Boolean = dao.insertWithinLimit(entity, limit)

    override suspend fun remove(
        id: String,
        updatedAt: Long,
    ) {
        dao.remove(id, updatedAt)
    }

    override suspend fun markSynced(
        id: String,
        updatedAt: Long,
    ) {
        dao.markSynced(id, updatedAt)
    }

    override suspend fun applyRemote(entities: List<BillAttachmentEntity>) {
        dao.applyRemote(entities)
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverAttachmentIds: Set<String>,
    ) {
        dao.deleteGoneFromServer(
            ownerUserId = ownerUserId,
            serverAttachmentIds = serverAttachmentIds,
        )
    }
}
