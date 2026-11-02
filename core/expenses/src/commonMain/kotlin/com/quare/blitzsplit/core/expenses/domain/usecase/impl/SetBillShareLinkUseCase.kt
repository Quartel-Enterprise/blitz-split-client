package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.repository.BillShareLinkRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.SetBillShareLink

class SetBillShareLinkUseCase(
    private val repository: BillShareLinkRepository,
) : SetBillShareLink {
    override suspend fun invoke(
        expenseId: String,
        visibility: BillLinkVisibility,
    ): Result<BillShareLink?> = repository.updateLink(expenseId, visibility)
}
