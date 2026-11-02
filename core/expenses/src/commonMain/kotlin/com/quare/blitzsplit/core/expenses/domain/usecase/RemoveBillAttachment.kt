package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface RemoveBillAttachment {
    suspend operator fun invoke(id: String): Result<Unit>
}
