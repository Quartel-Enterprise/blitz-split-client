package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.repository.BillShareLinkRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/*
 * In-memory BillShareLinkRepository, as the server keeps the links: each bill's link by its expense id, and what each
 * code shows. The state is public so a test can set and read it, and the failure knobs are public vars.
 */
class FakeBillShareLinkRepository(
    initialLinks: Map<String, BillShareLink> = emptyMap(),
    initialSharedBills: Map<String, SharedBill> = emptyMap(),
) : BillShareLinkRepository {
    // Why: by expense id.
    val links = MutableStateFlow(initialLinks)

    // Why: by code; a code missing here reads as NotFound, as the server answers it.
    val sharedBills = MutableStateFlow(initialSharedBills)

    // Why: the bills the server no longer has (or the caller isn't in their group): updateLink answers null.
    val goneExpenseIds = mutableSetOf<String>()

    // Why: the code the next new link gets; changing who may open a link keeps its code.
    var nextCode = NEW_CODE

    val loadedLinkIds = mutableListOf<String>()
    val updatedLinks = mutableListOf<Pair<String, BillLinkVisibility>>()
    val disabledExpenseIds = mutableListOf<String>()
    val loadedCodes = mutableListOf<String>()
    val postedComments = mutableListOf<PostedSharedBillComment>()
    val editedComments = mutableListOf<Triple<String, String, String>>()
    val deletedComments = mutableListOf<Pair<String, String>>()
    val likedComments = mutableListOf<Triple<String, String, Boolean>>()
    var changeOutcome = SharedBillComment.POSTED
    var changeError: Throwable? = null

    var commentOutcome = SharedBillComment.POSTED
    var getLinkError: Throwable? = null
    var updateError: Throwable? = null
    var disableError: Throwable? = null
    var sharedBillError: Throwable? = null
    var commentError: Throwable? = null

    // Why: holds a load until the test completes it, to see what shows while it runs.
    var loadGate: CompletableDeferred<Unit>? = null

    // Why: holds a change of the link, or a comment, until the test completes it.
    var updateGate: CompletableDeferred<Unit>? = null
    var commentGate: CompletableDeferred<Unit>? = null

    // Why: holds an edit, a deletion or a like of a comment until the test completes it.
    var changeGate: CompletableDeferred<Unit>? = null

    override suspend fun fetchLink(expenseId: String): Result<BillShareLink?> {
        loadedLinkIds += expenseId
        loadGate?.await()
        getLinkError?.let { return Result.failure(it) }
        return Result.success(links.value[expenseId])
    }

    override suspend fun updateLink(
        expenseId: String,
        visibility: BillLinkVisibility,
    ): Result<BillShareLink?> {
        updatedLinks += expenseId to visibility
        updateGate?.await()
        updateError?.let { return Result.failure(it) }
        if (expenseId in goneExpenseIds) return Result.success(null)
        val link = BillShareLink(
            code = links.value[expenseId]?.code ?: nextCode,
            visibility = visibility,
        )
        links.update { it + (expenseId to link) }
        return Result.success(link)
    }

    override suspend fun disableLink(expenseId: String): Result<Unit> {
        disabledExpenseIds += expenseId
        disableError?.let { return Result.failure(it) }
        links.update { it - expenseId }
        return Result.success(Unit)
    }

    override suspend fun fetchSharedBill(code: String): Result<SharedBill> {
        loadedCodes += code
        loadGate?.await()
        sharedBillError?.let { return Result.failure(it) }
        return Result.success(sharedBills.value[code] ?: SharedBill.NotFound)
    }

    override suspend fun createSharedBillComment(
        code: String,
        id: String,
        body: String,
        threadId: String?,
        replyToName: String?,
        authorName: String?,
    ): Result<SharedBillComment> {
        commentGate?.await()
        commentError?.let { return Result.failure(it) }
        postedComments += PostedSharedBillComment(code, id, body, threadId, replyToName, authorName)
        return Result.success(commentOutcome)
    }

    override suspend fun updateSharedBillComment(
        code: String,
        id: String,
        body: String,
    ): Result<SharedBillComment> {
        changeGate?.await()
        changeError?.let { return Result.failure(it) }
        editedComments += Triple(code, id, body)
        return Result.success(changeOutcome)
    }

    override suspend fun deleteSharedBillComment(
        code: String,
        id: String,
    ): Result<SharedBillComment> {
        changeGate?.await()
        changeError?.let { return Result.failure(it) }
        deletedComments += code to id
        return Result.success(changeOutcome)
    }

    override suspend fun updateSharedBillCommentLike(
        code: String,
        id: String,
        isLiked: Boolean,
    ): Result<SharedBillComment> {
        changeGate?.await()
        changeError?.let { return Result.failure(it) }
        likedComments += Triple(code, id, isLiked)
        return Result.success(changeOutcome)
    }

    // Why: what a comment through a link sent to the server.
    data class PostedSharedBillComment(
        val code: String,
        val id: String,
        val body: String,
        val threadId: String?,
        val replyToName: String?,
        val authorName: String?,
    )

    companion object {
        const val NEW_CODE = "7Hk2pQ8x"
    }
}
