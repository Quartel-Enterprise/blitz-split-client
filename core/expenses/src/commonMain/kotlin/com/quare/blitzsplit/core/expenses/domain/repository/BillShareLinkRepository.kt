package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment

// Why: a bill's link and what it shows, online only: the server keeps it, never the device.
interface BillShareLinkRepository {
    // Why: null while the bill has no link.
    suspend fun fetchLink(expenseId: String): Result<BillShareLink?>

    // Why: turns the link on, or changes who may open it keeping its code; null when the bill is gone.
    suspend fun updateLink(
        expenseId: String,
        visibility: BillLinkVisibility,
    ): Result<BillShareLink?>

    suspend fun disableLink(expenseId: String): Result<Unit>

    suspend fun fetchSharedBill(code: String): Result<SharedBill>

    suspend fun createSharedBillComment(
        code: String,
        id: String,
        body: String,
        threadId: String?,
        replyToName: String?,
        authorName: String?,
    ): Result<SharedBillComment>

    // Why: the caller's own comment, through the link; NEEDS_NAME never comes back.
    suspend fun updateSharedBillComment(
        code: String,
        id: String,
        body: String,
    ): Result<SharedBillComment>

    suspend fun deleteSharedBillComment(
        code: String,
        id: String,
    ): Result<SharedBillComment>

    suspend fun updateSharedBillCommentLike(
        code: String,
        id: String,
        isLiked: Boolean,
    ): Result<SharedBillComment>
}
