package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor

fun interface DeleteCustomColor {
    // Why: "Delete" after a long press on one of the person's colors; the picker asks FindCustomColorUser first.
    suspend operator fun invoke(color: CustomColor): Result<Unit>
}
