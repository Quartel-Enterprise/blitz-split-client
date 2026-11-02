package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink

fun interface LoadBillShareLink {
    // Why: the bill's link as the server has it, null while it has none; online only.
    suspend operator fun invoke(expenseId: String): Result<BillShareLink?>
}
