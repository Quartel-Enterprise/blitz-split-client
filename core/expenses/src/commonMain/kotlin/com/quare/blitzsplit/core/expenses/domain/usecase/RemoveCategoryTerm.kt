package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface RemoveCategoryTerm {
    // Why: no confirmation: the notice brings "Desfazer" (9.1d).
    suspend operator fun invoke(term: String): Result<Unit>
}
