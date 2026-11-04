package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface DeleteBillComment {
    suspend operator fun invoke(id: String): Result<Unit>
}
