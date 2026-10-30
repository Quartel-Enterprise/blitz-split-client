package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface DeleteExpense {
    // Why: "Apagar conta", which every member may do.
    suspend operator fun invoke(expenseId: String): Result<Unit>
}
