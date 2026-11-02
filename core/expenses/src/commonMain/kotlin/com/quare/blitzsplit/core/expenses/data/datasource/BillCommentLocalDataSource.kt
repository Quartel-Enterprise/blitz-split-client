package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
import kotlinx.coroutines.flow.Flow

interface BillCommentLocalDataSource {
    fun observeComments(expenseId: String): Flow<List<BillCommentEntity>>

    fun observeHasPending(): Flow<Boolean>

    fun observePendingIds(): Flow<List<String>>

    suspend fun getPendingComments(ownerUserId: String): List<BillCommentEntity>

    suspend fun insertComment(entity: BillCommentEntity)

    suspend fun applyRemote(entities: List<BillCommentEntity>)

    suspend fun markSynced(ids: List<String>)

    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverCommentIds: Set<String>,
    )
}
