package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.repository.BillShareLinkRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteSharedBillComment

class DeleteSharedBillCommentUseCase(
    private val repository: BillShareLinkRepository,
) : DeleteSharedBillComment {
    override suspend fun invoke(
        code: String,
        id: String,
    ): Result<SharedBillComment> = repository.deleteSharedBillComment(code, id)
}
