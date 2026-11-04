package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment

fun interface EditSharedBillComment {
    // Why: the caller's own comment, through a bill's link (7.3h), online only as commenting there is.
    suspend operator fun invoke(
        code: String,
        id: String,
        body: String,
    ): Result<SharedBillComment>
}
