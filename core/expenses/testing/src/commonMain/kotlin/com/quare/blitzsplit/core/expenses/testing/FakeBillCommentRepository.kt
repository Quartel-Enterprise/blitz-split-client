package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLike
import com.quare.blitzsplit.core.expenses.domain.repository.BillCommentRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// In-memory BillCommentRepository: the state is public so a test can set and read it, and the failure knobs are public vars.
class FakeBillCommentRepository(
    initialComments: List<BillComment> = emptyList(),
) : BillCommentRepository {
    val comments = MutableStateFlow(initialComments)
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var addError: Throwable? = null
    var addGate: CompletableDeferred<Unit>? = null
    var changeError: Throwable? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null

    override fun observeComments(expenseId: String): Flow<List<BillComment>> =
        comments.map { list -> list.filter { it.expenseId == expenseId }.sortedBy { it.postedAt } }

    override fun observeHasPendingChanges(): Flow<Boolean> = comments.map { list -> list.any { it.isPending } }

    override fun observePendingIds(): Flow<List<String>> =
        comments.map { list -> list.filter { it.isPending }.map { it.id } }

    override suspend fun addComment(comment: BillComment): Result<Unit> {
        addGate?.await()
        addError?.let { return Result.failure(it) }
        comments.update { it + comment }
        return Result.success(Unit)
    }

    override suspend fun editComment(
        id: String,
        body: String,
        editedAt: Long,
    ): Result<Unit> = change(id) { it.copy(body = body, editedAt = editedAt) }

    override suspend fun deleteComment(id: String): Result<Unit> = change(id) { it.copy(body = "", isDeleted = true) }

    override suspend fun setLike(
        commentId: String,
        groupId: String,
        isLiked: Boolean,
        memberId: String?,
        likerName: String?,
        at: Long,
    ): Result<Unit> = change(commentId) { comment ->
        val others = comment.likes.filterNot { it.isMine }
        val mine = BillCommentLike(memberId = memberId, name = likerName, isMine = true)
        comment.copy(likes = if (isLiked) others + mine else others)
    }

    private fun change(
        id: String,
        transform: (BillComment) -> BillComment,
    ): Result<Unit> {
        changeError?.let { return Result.failure(it) }
        if (comments.value.none { it.id == id }) return Result.failure(IllegalStateException("No comment $id"))
        comments.update { list -> list.map { if (it.id == id) transform(it).copy(isPending = true) else it } }
        return Result.success(Unit)
    }

    override suspend fun uploadPending(userId: String): Result<Unit> {
        uploadedUserIds += userId
        pushError?.let { return Result.failure(it) }
        comments.update { list -> list.map { it.copy(isPending = false) } }
        return Result.success(Unit)
    }

    override suspend fun download(userId: String): Result<Unit> {
        downloadedUserIds += userId
        pullError?.let { return Result.failure(it) }
        return Result.success(Unit)
    }
}
