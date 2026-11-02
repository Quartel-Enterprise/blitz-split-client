package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.repository.BillShareLinkRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.LoadBillShareLink

class LoadBillShareLinkUseCase(
    private val repository: BillShareLinkRepository,
) : LoadBillShareLink {
    override suspend fun invoke(expenseId: String): Result<BillShareLink?> = repository.fetchLink(expenseId)
}
