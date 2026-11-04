package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.BillCommentLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.BillCommentRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.BillCommentMapper
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.repository.BillCommentRepository
import com.quare.blitzsplit.core.provider.room.entity.BillCommentLikeEntity
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class BillCommentRepositoryImpl(
    private val local: BillCommentLocalDataSource,
    private val remote: BillCommentRemoteDataSource,
    private val mapper: BillCommentMapper,
) : BillCommentRepository {
    override fun observeComments(expenseId: String): Flow<List<BillComment>> = combine(
        local.observeComments(expenseId),
        local.observeLikes(expenseId),
    ) { entities, likes ->
        val likesByComment = likes.groupBy { it.commentId }
        entities.map { entity -> mapper.toDomain(entity, likesByComment[entity.id].orEmpty()) }
    }.distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    override fun observePendingIds(): Flow<List<String>> = combine(
        local.observePendingIds(),
        local.observePendingLikeVersions(),
    ) { commentIds, likeVersions -> commentIds + likeVersions.map { "$LIKE_PREFIX$it" } }.distinctUntilChanged()

    override suspend fun addComment(comment: BillComment): Result<Unit> = suspendRunCatching {
        local.insertComment(mapper.toEntity(comment))
    }

    override suspend fun updateComment(
        id: String,
        body: String,
        editedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        check(local.updateComment(id, body, editedAt)) { "No comment $id to edit" }
    }

    override suspend fun deleteComment(id: String): Result<Unit> = suspendRunCatching {
        check(local.deleteComment(id)) { "No comment $id" }
    }

    override suspend fun updateLike(
        commentId: String,
        groupId: String,
        isLiked: Boolean,
        memberId: String?,
        likerName: String?,
        at: Long,
    ): Result<Unit> = suspendRunCatching {
        local.upsertMyLike(
            BillCommentLikeEntity(
                commentId = commentId,
                likerKey = BillCommentLikeEntity.ME,
                groupId = groupId,
                memberId = memberId,
                likerName = likerName,
                isLiked = isLiked,
                updatedAt = at,
                pendingSync = true,
            ),
        )
    }

    /*
     * Why: only the comments of this user's groups, after their bills went up in the same sync, then the likes, after
     * their comments. One the server drops (its bill deleted meanwhile) counts as synced and leaves the device on the
     * next download.
     */
    override suspend fun uploadPending(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.getPendingComments(userId)
        if (pending.isNotEmpty()) {
            remote.upsertComments(pending.map { mapper.toDto(it, userId) })
            local.markSynced(pending)
        }
        val likes = local.getPendingLikes(userId)
        if (likes.isNotEmpty()) {
            remote.upsertLikes(likes.map { mapper.toDto(it, userId) })
            local.markLikesSynced(likes)
        }
    }

    override suspend fun download(userId: String): Result<Unit> = suspendRunCatching {
        val comments = remote.fetchComments()
        local.applyRemote(comments.map(mapper::toEntity))
        local.deleteGoneFromServer(
            ownerUserId = userId,
            serverCommentIds = comments.map { it.id }.toSet(),
        )
        local.replaceSyncedLikes(userId, remote.fetchLikes().map { mapper.toEntity(it, userId) })
    }

    private companion object {
        // Why: a like's version among the comments' ids, which are UUIDs and so never look like it.
        const val LIKE_PREFIX = "like:"
    }
}
