package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import kotlinx.coroutines.flow.Flow

interface BillCommentRepository {
    // Why: a bill's comments in the order they were written, deleted ones included, each with its likes.
    fun observeComments(expenseId: String): Flow<List<BillComment>>

    fun observeHasPendingChanges(): Flow<Boolean>

    /*
     * Why: the comments and likes waiting to go up, so a new one, an edit or a toggle triggers a sync after a failed
     * push.
     */
    fun observePendingIds(): Flow<List<String>>

    // Why: on the device first, offline included (7.3f); the sync takes it up after its bill.
    suspend fun addComment(comment: BillComment): Result<Unit>

    // Why: the author's new text, offline too; fails when the comment is gone or deleted.
    suspend fun updateComment(
        id: String,
        body: String,
        editedAt: Long,
    ): Result<Unit>

    // Why: a tombstone, offline too; fails when the comment is gone.
    suspend fun deleteComment(id: String): Result<Unit>

    // Why: this device's user's like or its undoing, offline too; fails when the comment is gone.
    suspend fun updateLike(
        commentId: String,
        groupId: String,
        isLiked: Boolean,
        memberId: String?,
        likerName: String?,
        at: Long,
    ): Result<Unit>

    suspend fun uploadPending(userId: String): Result<Unit>

    suspend fun download(userId: String): Result<Unit>
}
