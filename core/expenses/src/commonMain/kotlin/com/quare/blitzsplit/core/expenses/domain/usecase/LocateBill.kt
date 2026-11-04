package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.BillLocation

fun interface LocateBill {
    // Why: the device first, then the server, whose row level security only answers the user's groups.
    suspend operator fun invoke(expenseId: String): BillLocation
}
