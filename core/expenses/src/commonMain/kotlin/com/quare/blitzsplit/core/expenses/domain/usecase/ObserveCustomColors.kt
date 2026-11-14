package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor
import kotlinx.coroutines.flow.Flow

fun interface ObserveCustomColors {
    // Why: the person's palette, the swatches after the twelve named colors in every category color picker.
    operator fun invoke(): Flow<List<CustomColor>>
}
