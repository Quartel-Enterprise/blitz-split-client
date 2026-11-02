package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.SharedBill

fun interface LoadSharedBill {
    // Why: what blitzsplit.com/c/<code> shows, read from the server each time.
    suspend operator fun invoke(code: String): Result<SharedBill>
}
