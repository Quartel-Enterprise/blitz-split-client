package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BillLocation
import com.quare.blitzsplit.core.expenses.domain.model.StoredBill
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.usecase.LocateBill

class LocateBillUseCase(
    private val repository: ExpenseRepository,
) : LocateBill {
    override suspend fun invoke(expenseId: String): BillLocation {
        repository.findDeviceBill(expenseId)?.let { return locationOf(it, isOnDevice = true) }
        return repository.fetchServerBill(expenseId).fold(
            onSuccess = { bill -> bill?.let { locationOf(it, isOnDevice = false) } ?: BillLocation.OutOfReach },
            onFailure = { BillLocation.Unknown },
        )
    }

    private fun locationOf(
        bill: StoredBill,
        isOnDevice: Boolean,
    ): BillLocation = if (bill.isDeleted) {
        BillLocation.Deleted(bill.groupId)
    } else {
        BillLocation.Found(bill.groupId, isOnDevice)
    }
}
