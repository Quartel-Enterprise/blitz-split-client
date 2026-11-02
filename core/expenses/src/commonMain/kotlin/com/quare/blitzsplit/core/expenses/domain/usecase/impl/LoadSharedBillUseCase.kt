package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.repository.BillShareLinkRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.LoadSharedBill

class LoadSharedBillUseCase(
    private val repository: BillShareLinkRepository,
) : LoadSharedBill {
    override suspend fun invoke(code: String): Result<SharedBill> = repository.fetchSharedBill(code)
}
