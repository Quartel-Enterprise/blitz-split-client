package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface DeletePayingMethod {
    // Why: the bills marked with it read as unmarked from then on.
    suspend operator fun invoke(methodId: String): Result<Unit>
}
