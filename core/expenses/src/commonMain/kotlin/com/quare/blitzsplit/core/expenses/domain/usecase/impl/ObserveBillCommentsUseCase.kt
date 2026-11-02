package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.repository.BillCommentRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveBillComments
import kotlinx.coroutines.flow.Flow

class ObserveBillCommentsUseCase(
    private val repository: BillCommentRepository,
) : ObserveBillComments {
    override fun invoke(expenseId: String): Flow<List<BillComment>> = repository.observeComments(expenseId)
}
