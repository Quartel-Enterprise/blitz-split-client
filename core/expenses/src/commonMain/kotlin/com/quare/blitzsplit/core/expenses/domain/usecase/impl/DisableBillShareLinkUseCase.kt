package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.repository.BillShareLinkRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.DisableBillShareLink

class DisableBillShareLinkUseCase(
    private val repository: BillShareLinkRepository,
) : DisableBillShareLink {
    override suspend fun invoke(expenseId: String): Result<Unit> = repository.disableLink(expenseId)
}
