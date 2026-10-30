package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: an amount typed as people write it, in Portuguese or English ("1.234,56", "1,234.56", "12,5", "12"), to a
 * whole number of the currency's minor unit. A separator followed by at most the currency's fraction digits is the
 * decimal one; any other separator only groups thousands.
 */
object AmountInput {
    // Why: null for text that is no amount; a currency without cents reads any separator as grouping.
    fun parse(
        text: String,
        currency: GroupCurrency,
    ): Long? {
        val cleaned = text.filterNot { it.isWhitespace() }
        if (cleaned.isEmpty() || cleaned.any { !it.isDigit() && it !in SEPARATORS }) return null
        val lastSeparator = cleaned.indexOfLast { it in SEPARATORS }
        val decimals = if (lastSeparator < 0) "" else cleaned.substring(lastSeparator + 1)
        val isDecimal = lastSeparator >= 0 &&
            currency.fractionDigits > 0 &&
            decimals.length <= currency.fractionDigits &&
            cleaned.count { it == cleaned[lastSeparator] } == 1
        val wholeText = if (isDecimal) cleaned.substring(0, lastSeparator) else cleaned
        val whole = wholeText.filter { it.isDigit() }
        val fraction = if (isDecimal) decimals else ""
        if (whole.isEmpty() && fraction.isEmpty()) return null
        // Why: more digits than any amount the rules allow can't be a Long, nor a valid bill.
        if (whole.length > MAX_DIGITS) return null
        val factor = ExpenseRules.minorPerMajorFor(currency)
        val wholeMinor = (whole.ifEmpty { "0" }.toLong()) * factor
        val fractionMinor = fraction.padEnd(currency.fractionDigits, '0').ifEmpty { "0" }.toLong()
        return wholeMinor + fractionMinor
    }

    // Why: an amount the app fills in a field, as the person would type it: "62,40" with the separator they use.
    fun format(
        amountMinor: Long,
        currency: GroupCurrency,
        decimalSeparator: Char,
    ): String {
        val factor = ExpenseRules.minorPerMajorFor(currency)
        val whole = (amountMinor / factor).toString()
        if (currency.fractionDigits == 0) return whole
        val fraction = (amountMinor % factor).toString().padStart(currency.fractionDigits, '0')
        return whole + decimalSeparator + fraction
    }

    // Why: a whole number typed in a field of percentages or parts.
    fun parseWhole(text: String): Long? = text
        .filterNot { it.isWhitespace() }
        .takeIf { it.isNotEmpty() && it.length <= MAX_DIGITS && it.all(Char::isDigit) }
        ?.toLong()

    private val SEPARATORS = setOf(',', '.')

    private const val MAX_DIGITS = 12
}
