package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment

fun interface LikeSharedBillComment {
    // Why: anyone who may comment through a bill's link likes any comment there, or undoes their like; online only.
    suspend operator fun invoke(
        code: String,
        id: String,
        isLiked: Boolean,
    ): Result<SharedBillComment>
}
