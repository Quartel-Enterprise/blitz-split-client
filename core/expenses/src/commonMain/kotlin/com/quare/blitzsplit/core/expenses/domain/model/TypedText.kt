package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: what the amount, percentage and parts fields keep of what was typed, the same in Rachar conta, Dividir,
 * Registrar pagamento and the group's "Divisão padrão", so their rules never drift apart.
 */
object TypedText {
    /*
     * Why: an amount (the bill's, a payment's, each of Valores', an item's price, the couvert, an income of "Pela
     * renda") as a cash machine takes it: the digits come in from the right with the currency's decimals fixed, so
     * typing 1, 2, 3 reads "0,01", "0,12", "1,23", a separator typed changes nothing, and nobody has to know whether
     * the comma or the dot is the decimal one. Anything else, a paste or an autofill, is read as an amount instead
     * ("12,5" is "12,50", "1,234.56" or "R$ 1.234,56" the same), since it comes whole from elsewhere. A digit past the
     * digits the largest amount allowed has, or a paste that is no amount, keeps the field as it was.
     */
    fun toAmountText(
        previous: String,
        typed: String,
        currency: GroupCurrency,
        decimalSeparator: Char,
    ): String {
        val amountMinor = if (isKeystroke(previous, typed)) {
            typedAmountOf(typed, currency)
        } else {
            pastedAmountOf(previous, typed, currency)
        } ?: return previous
        if (amountMinor == 0L) return ""
        return AmountInput.format(amountMinor, currency, amountSeparatorOf(decimalSeparator))
    }

    /*
     * Why: an amount field's text once the form moves to another currency: the same number, its decimals cut to the
     * new currency's ("12,50" in reais is "12" in yen, and "1.250" in yen "1.250,00" in reais), not its digits read
     * again, which would make R$ 90,00 ¥ 9.000.
     */
    fun toCurrency(
        text: String,
        from: GroupCurrency,
        to: GroupCurrency,
        decimalSeparator: Char,
    ): String {
        if (from == to) return text
        val amountMinor = AmountInput.parse(text, from) ?: return ""
        val converted = amountMinor * ExpenseRules.minorPerMajorFor(to) / ExpenseRules.minorPerMajorFor(from)
        return if (converted == 0L) "" else AmountInput.format(converted, to, amountSeparatorOf(decimalSeparator))
    }

    // Why: a whole percentage, 0 to 100.
    fun toPercentText(text: String): String = text.filter { it.isDigit() }.take(PERCENT_MAX_LENGTH)

    fun toSplitValueText(
        previous: String,
        typed: String,
        mode: SplitMode,
        currency: GroupCurrency,
        decimalSeparator: Char,
    ): String = if (mode == SplitMode.AMOUNT) {
        toAmountText(previous, typed, currency, decimalSeparator)
    } else {
        toPercentText(typed)
    }

    // Why: a language whose separator is neither (Arabic's "٫") would fill in text AmountInput can't read back.
    fun amountSeparatorOf(decimalSeparator: Char): Char =
        decimalSeparator.takeIf { it in amountSeparators } ?: FALLBACK_SEPARATOR

    /*
     * Why: one key, a digit more or less than the field held, or the field selected whole and one key typed. Digits,
     * not the text, since a field may still show a separator or letter refused before (a Mac's keyboard), and without
     * the zeros the mask pads with in front, since a field whose text lags the ViewModel's by a key ("0,012" while it
     * holds "0,12", two keys typed in one frame) still sends one key more. An empty field takes one key at a time, so
     * more is a paste ("0,5" is 50 cents).
     */
    private fun isKeystroke(
        previous: String,
        typed: String,
    ): Boolean {
        if (typed.length <= 1) return true
        val before = previous.filter { it.isDigit() }.trimStart('0')
        val after = typed.filter { it.isDigit() }.trimStart('0')
        if (after == before) return true
        if (previous.isEmpty()) return false
        val (longer, shorter) = if (after.length > before.length) after to before else before to after
        if (longer.length - shorter.length != 1) return false
        val firstDifference = shorter.indices.firstOrNull { longer[it] != shorter[it] } ?: shorter.length
        return longer.removeRange(firstDifference, firstDifference + 1) == shorter
    }

    /*
     * Why: every digit is a minor unit's, so the field holds no more of them than the largest amount allowed has; one
     * over it with as many digits stays, for the field to say it is too much.
     */
    private fun typedAmountOf(
        typed: String,
        currency: GroupCurrency,
    ): Long? {
        val digits = typed.filter { it.isDigit() }.trimStart('0')
        return when {
            digits.isEmpty() -> 0L
            digits.length > maxDigitsOf(currency) -> null
            else -> digits.toLong()
        }
    }

    private fun maxDigitsOf(currency: GroupCurrency): Int = ExpenseRules.maxAmountMinorFor(currency).toString().length

    // Why: pasted after what the field held, only what came in counts; pasted over it, all of it.
    private fun pastedAmountOf(
        previous: String,
        typed: String,
        currency: GroupCurrency,
    ): Long? {
        val pasted = if (previous.isNotEmpty() && typed.startsWith(previous)) typed.drop(previous.length) else typed
        return AmountInput
            .parse(pasted.filter { it.isDigit() || it in amountSeparators }, currency)
            ?.takeIf { it.toString().length <= maxDigitsOf(currency) }
    }

    private val amountSeparators = setOf(',', '.')

    private const val PERCENT_MAX_LENGTH = 3
    private const val FALLBACK_SEPARATOR = '.'
}
