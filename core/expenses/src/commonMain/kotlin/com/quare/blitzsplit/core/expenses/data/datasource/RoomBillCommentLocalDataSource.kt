package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.BillCommentDao
import com.quare.blitzsplit.core.provider.room.dao.BillCommentLikeDao
import com.quare.blitzsplit.core.provider.room.entity.BillCommentEntity
import com.quare.blitzsplit.core.provider.room.entity.BillCommentLikeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoomBillCommentLocalDataSource(
    private val dao: BillCommentDao,
    private val likeDao: BillCommentLikeDao,
) : BillCommentLocalDataSource {
    override fun observeComments(expenseId: String): Flow<List<BillCommentEntity>> = dao.observeComments(expenseId)

    override fun observeLikes(expenseId: String): Flow<List<BillCommentLikeEntity>> = likeDao.observeLikes(expenseId)

    override fun observeHasPending(): Flow<Boolean> = combine(
        dao.observeHasPending(),
        likeDao.observeHasPending(),
    ) { hasPendingComments, hasPendingLikes -> hasPendingComments || hasPendingLikes }

    override fun observePendingIds(): Flow<List<String>> = dao.observePendingIds()

    override fun observePendingLikeVersions(): Flow<List<Long>> = likeDao.observePendingVersions()

    override suspend fun getPendingComments(ownerUserId: String): List<BillCommentEntity> =
        dao.getPendingComments(ownerUserId)

    override suspend fun getPendingLikes(ownerUserId: String): List<BillCommentLikeEntity> =
        likeDao.getPendingLikes(ownerUserId)

    override suspend fun insertComment(entity: BillCommentEntity) {
        dao.insertCommentOnBill(entity)
    }

    override suspend fun editComment(
        id: String,
        body: String,
        editedAt: Long,
    ): Boolean = dao.editComment(id, body, editedAt) > 0

    override suspend fun deleteComment(id: String): Boolean = dao.deleteComment(id) > 0

    override suspend fun upsertMyLike(entity: BillCommentLikeEntity) {
        likeDao.upsertMine(entity)
    }

    override suspend fun applyRemote(entities: List<BillCommentEntity>) {
        dao.applyRemoteToBills(entities)
    }

    override suspend fun replaceSyncedLikes(
        ownerUserId: String,
        entities: List<BillCommentLikeEntity>,
    ) {
        likeDao.replaceSynced(ownerUserId, entities)
    }

    override suspend fun markSynced(entities: List<BillCommentEntity>) {
        dao.markSynced(entities)
    }

    override suspend fun markLikesSynced(entities: List<BillCommentLikeEntity>) {
        likeDao.markSynced(entities)
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
