package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.SplitMode

// Why: what the fields of Rachar conta and Dividir keep of what was typed.
internal object TypedText {
    val amountSeparators = setOf(',', '.')

    // Why: the bill's amount and each of Valores'.
    fun toAmountText(text: String): String =
        text.filter { it.isDigit() || it in amountSeparators }.take(AMOUNT_MAX_LENGTH)

    fun toSplitValueText(
        text: String,
        mode: SplitMode,
    ): String = if (mode == SplitMode.AMOUNT) {
        toAmountText(text)
    } else {
        text.filter { it.isDigit() }.take(PERCENT_MAX_LENGTH)
    }

    // Why: "100.000.000,00" with room for the separators someone types.
    private const val AMOUNT_MAX_LENGTH = 16
    private const val PERCENT_MAX_LENGTH = 3
}
