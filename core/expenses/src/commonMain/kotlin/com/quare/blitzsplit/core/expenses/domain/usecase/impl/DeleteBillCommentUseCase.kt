package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.BillCommentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteBillComment

class DeleteBillCommentUseCase(
    private val repository: BillCommentRepository,
) : DeleteBillComment {
    override suspend fun invoke(id: String): Result<Unit> = repository.deleteComment(id)
}
