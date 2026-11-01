package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: what the amount, percentage and parts fields keep of what was typed, the same in Rachar conta, Dividir and
 * the group's "Divisão padrão", so their rules never drift apart.
 */
object TypedText {
    // Why: an amount, the bill's, each of Valores' or an income of "Pela renda".
    fun toAmountText(text: String): String =
        text.filter { it.isDigit() || it in amountSeparators }.take(AMOUNT_MAX_LENGTH)

    // Why: a whole percentage, 0 to 100.
    fun toPercentText(text: String): String = text.filter { it.isDigit() }.take(PERCENT_MAX_LENGTH)

    fun toSplitValueText(
        text: String,
        mode: SplitMode,
    ): String = if (mode == SplitMode.AMOUNT) toAmountText(text) else toPercentText(text)

    // Why: a language whose separator is neither (Arabic's "٫") would fill in text AmountInput can't read back.
    fun amountSeparatorOf(decimalSeparator: Char): Char =
        decimalSeparator.takeIf { it in amountSeparators } ?: FALLBACK_SEPARATOR

    private val amountSeparators = setOf(',', '.')

    // Why: "100.000.000,00" with room for the separators someone types.
    private const val AMOUNT_MAX_LENGTH = 16
    private const val PERCENT_MAX_LENGTH = 3
    private const val FALLBACK_SEPARATOR = '.'
}
