package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface MarkBillPayingMethod {
    // Why: "Paid with": which of their methods this device's user paid the bill with; null clears it.
    suspend operator fun invoke(
        expenseId: String,
        methodId: String?,
    ): Result<Unit>
}
