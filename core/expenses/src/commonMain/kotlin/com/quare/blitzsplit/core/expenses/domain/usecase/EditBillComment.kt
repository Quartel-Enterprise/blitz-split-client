package com.quare.blitzsplit.core.expenses.domain.usecase

fun interface EditBillComment {
    // Why: only its author's, which the screen offers; the server refuses anyone else's anyway.
    suspend operator fun invoke(
        id: String,
        body: String,
    ): Result<Unit>
}
