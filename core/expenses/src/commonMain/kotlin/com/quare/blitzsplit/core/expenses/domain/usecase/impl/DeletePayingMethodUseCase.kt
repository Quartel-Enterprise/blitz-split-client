package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.PayingMethodRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.DeletePayingMethod
import com.quare.blitzsplit.core.utils.suspendRunCatching

class DeletePayingMethodUseCase(
    private val repository: PayingMethodRepository,
) : DeletePayingMethod {
    override suspend fun invoke(methodId: String): Result<Unit> = suspendRunCatching {
        repository.deleteMethod(methodId)
    }
}
