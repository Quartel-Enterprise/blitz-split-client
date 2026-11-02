package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentRules
import com.quare.blitzsplit.core.expenses.domain.repository.BillCommentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.AddBillComment
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlin.uuid.Uuid

// Why: on the device right away, offline included; the others see it once synced (7.3f).
class AddBillCommentUseCase(
    private val repository: BillCommentRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : AddBillComment {
    override suspend fun invoke(
        expenseId: String,
        groupId: String,
        body: String,
        threadId: String?,
        replyToName: String?,
        authorMemberId: String?,
        authorName: String?,
    ): Result<Unit> {
        if (!BillCommentRules.isBodyValid(body)) return Result.failure(IllegalArgumentException("Invalid comment"))
        return repository.addComment(
            BillComment(
                id = Uuid.random().toString(),
                expenseId = expenseId,
                groupId = groupId,
                authorUserId = null,
                authorMemberId = authorMemberId,
                authorName = authorName,
                threadId = threadId,
                replyToName = replyToName,
                body = body.trim(),
                viaLink = false,
                postedAt = currentTimeMillis(),
                isPending = true,
            ),
        )
    }
}
