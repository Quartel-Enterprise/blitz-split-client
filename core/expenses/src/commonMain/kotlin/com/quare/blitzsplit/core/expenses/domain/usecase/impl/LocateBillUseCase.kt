package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillLocation
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.LocateBill
import kotlinx.coroutines.flow.first

class LocateBillUseCase(
    private val repository: ExpenseRepository,
) : LocateBill {
    override suspend fun invoke(expenseId: String): BillLocation {
        repository.observeExpense(expenseId).first()?.let { return BillLocation.Found(it.groupId) }
        return repository.fetchServerBill(expenseId).fold(
            onSuccess = { bill ->
                when {
                    bill == null -> BillLocation.OutOfReach
                    bill.isDeleted -> BillLocation.Deleted(bill.groupId)
                    else -> BillLocation.Found(bill.groupId)
                }
            },
            onFailure = { BillLocation.Unknown },
        )
    }
}
