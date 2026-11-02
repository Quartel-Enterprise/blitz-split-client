package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface DeleteGroupCategory {
    // Why: "Apagar categoria" (7.8f): its bills read as its parent, or as "Outros" for a main one.
    suspend operator fun invoke(categoryId: String): Result<Unit>
}
