package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillCommentRules
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.repository.BillShareLinkRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.EditSharedBillComment

class EditSharedBillCommentUseCase(
    private val repository: BillShareLinkRepository,
) : EditSharedBillComment {
    override suspend fun invoke(
        code: String,
        id: String,
        body: String,
    ): Result<SharedBillComment> {
        if (!BillCommentRules.isBodyValid(body)) return Result.failure(IllegalArgumentException("Invalid comment"))
        return repository.editSharedBillComment(code, id, body.trim())
    }
}
