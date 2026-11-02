package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillCommentRules
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.repository.BillShareLinkRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.CommentOnSharedBill

// Why: online only, since whoever comments through a link may have no group on this device to queue it in.
class CommentOnSharedBillUseCase(
    private val repository: BillShareLinkRepository,
) : CommentOnSharedBill {
    override suspend fun invoke(
        code: String,
        id: String,
        body: String,
        threadId: String?,
        replyToName: String?,
        authorName: String?,
    ): Result<SharedBillComment> {
        if (!BillCommentRules.isBodyValid(body)) return Result.failure(IllegalArgumentException("Invalid comment"))
        return repository.createSharedBillComment(
            code = code,
            id = id,
            body = body.trim(),
            threadId = threadId,
            replyToName = replyToName,
            authorName = authorName?.trim()?.ifEmpty { null },
        )
    }
}
