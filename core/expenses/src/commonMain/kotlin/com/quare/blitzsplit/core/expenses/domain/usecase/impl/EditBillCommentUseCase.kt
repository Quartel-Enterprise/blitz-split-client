package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillCommentRules
import com.quare.blitzsplit.core.expenses.domain.repository.BillCommentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.EditBillComment
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

// Why: on the device right away, offline included, as a new comment; the newer edit wins on the server.
class EditBillCommentUseCase(
    private val repository: BillCommentRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : EditBillComment {
    override suspend fun invoke(
        id: String,
        body: String,
    ): Result<Unit> {
        if (!BillCommentRules.isBodyValid(body)) return Result.failure(IllegalArgumentException("Invalid comment"))
        return repository.editComment(id, body.trim(), currentTimeMillis())
    }
}
