package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.PayingMethodRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.MarkBillPayingMethod
import com.quare.blitzsplit.core.utils.suspendRunCatching

class MarkBillPayingMethodUseCase(
    private val repository: PayingMethodRepository,
) : MarkBillPayingMethod {
    override suspend fun invoke(
        expenseId: String,
        methodId: String?,
    ): Result<Unit> = suspendRunCatching {
        repository.markBill(expenseId, methodId)
    }
}
