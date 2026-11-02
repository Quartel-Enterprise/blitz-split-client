package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.BillAttachmentEntity
import kotlinx.coroutines.flow.Flow

interface BillAttachmentLocalDataSource {
    fun observeAttachments(expenseId: String): Flow<List<BillAttachmentEntity>>

    fun observeHasPending(): Flow<Boolean>

    fun observePendingVersions(): Flow<List<String>>

    suspend fun getPendingAttachmentIds(ownerUserId: String): List<String>

    suspend fun getAttachment(id: String): BillAttachmentEntity?

    suspend fun insertWithinLimit(
        entity: BillAttachmentEntity,
        limit: Int,
    ): Boolean

    suspend fun remove(
        id: String,
        updatedAt: Long,
    )

    suspend fun markSynced(
        id: String,
        updatedAt: Long,
    )

    suspend fun applyRemote(entities: List<BillAttachmentEntity>)

    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverAttachmentIds: Set<String>,
    )
}
