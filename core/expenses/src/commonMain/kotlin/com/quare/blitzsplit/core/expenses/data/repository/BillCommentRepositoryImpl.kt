package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.BillCommentLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.BillCommentRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.BillCommentMapper
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.repository.BillCommentRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class BillCommentRepositoryImpl(
    private val local: BillCommentLocalDataSource,
    private val remote: BillCommentRemoteDataSource,
    private val mapper: BillCommentMapper,
) : BillCommentRepository {
    override fun observeComments(expenseId: String): Flow<List<BillComment>> = local
        .observeComments(expenseId)
        .map { entities -> entities.map(mapper::toDomain) }
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    override fun observePendingIds(): Flow<List<String>> = local.observePendingIds().distinctUntilChanged()

    override suspend fun addComment(comment: BillComment): Result<Unit> = suspendRunCatching {
        local.insertComment(mapper.toEntity(comment))
    }

    /*
     * Why: only the comments of this user's groups, after their bills went up in the same sync. One the server drops
     * (its bill deleted meanwhile) counts as synced and leaves the device on the next download.
     */
    override suspend fun uploadPending(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.getPendingComments(userId)
        if (pending.isEmpty()) return@suspendRunCatching
        remote.insertComments(pending.map { mapper.toDto(it, userId) })
        local.markSynced(pending.map { it.id })
    }

    override suspend fun download(userId: String): Result<Unit> = suspendRunCatching {
        val comments = remote.fetchComments()
        local.applyRemote(comments.map(mapper::toEntity))
        local.deleteGoneFromServer(
            ownerUserId = userId,
            serverCommentIds = comments.map { it.id }.toSet(),
        )
    }
}
