package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
import com.quare.blitzsplit.core.provider.room.entity.BillCommentLikeEntity
import kotlinx.coroutines.flow.Flow

interface BillCommentLocalDataSource {
    fun observeComments(expenseId: String): Flow<List<BillCommentEntity>>

    fun observeLikes(expenseId: String): Flow<List<BillCommentLikeEntity>>

    fun observeHasPending(): Flow<Boolean>

    fun observePendingIds(): Flow<List<String>>

    fun observePendingLikeVersions(): Flow<List<Long>>

    suspend fun getPendingComments(ownerUserId: String): List<BillCommentEntity>

    suspend fun getPendingLikes(ownerUserId: String): List<BillCommentLikeEntity>

    suspend fun insertComment(entity: BillCommentEntity)

    // Why: false when the comment is gone, or already deleted.
    suspend fun editComment(
        id: String,
        body: String,
        editedAt: Long,
    ): Boolean

    // Why: false when the comment is gone.
    suspend fun deleteComment(id: String): Boolean

    suspend fun upsertMyLike(entity: BillCommentLikeEntity)

    suspend fun applyRemote(entities: List<BillCommentEntity>)

    suspend fun replaceSyncedLikes(
        ownerUserId: String,
        entities: List<BillCommentLikeEntity>,
    )

    suspend fun markSynced(entities: List<BillCommentEntity>)

    suspend fun markLikesSynced(entities: List<BillCommentLikeEntity>)

    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverCommentIds: Set<String>,
    )
}
