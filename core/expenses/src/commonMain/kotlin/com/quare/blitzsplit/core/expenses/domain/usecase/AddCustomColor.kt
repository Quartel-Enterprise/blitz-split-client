package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.CustomColor

fun interface AddCustomColor {
    /*
     * Why: "Add" in the "+" picker, on the device first, offline included. A color already in the palette is kept where
     * it is; a new one past CustomPalette.MAX_COLORS fails with CustomPaletteFullException.
     */
    suspend operator fun invoke(color: CustomColor): Result<Unit>
}
