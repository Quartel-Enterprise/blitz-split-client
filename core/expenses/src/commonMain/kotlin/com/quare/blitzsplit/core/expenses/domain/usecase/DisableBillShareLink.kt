package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface DisableBillShareLink {
    // Why: "Desativar link": the code stops working, and the next link is a new one.
    suspend operator fun invoke(expenseId: String): Result<Unit>
}
