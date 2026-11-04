package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment

fun interface DeleteSharedBillComment {
    // Why: the caller's own comment, through a bill's link, online only.
    suspend operator fun invoke(
        code: String,
        id: String,
    ): Result<SharedBillComment>
}
