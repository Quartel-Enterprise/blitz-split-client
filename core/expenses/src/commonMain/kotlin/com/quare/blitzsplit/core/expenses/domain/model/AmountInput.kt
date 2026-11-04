package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: an amount typed as people write it, in Portuguese or English ("1.234,56", "1,234.56", "12,5", "12"), to a
 * whole number of the currency's minor unit. A separator followed by at most the currency's fraction digits is the
 * decimal one; any other separator only groups thousands. Bitcoin in satoshis (`showsSats`) is typed in whole sats,
 * its minor unit, so it reads as a currency without cents.
 */
object AmountInput {
    // Why: null for text that is no amount; a currency without cents reads any separator as grouping.
    fun parse(
        text: String,
        currency: GroupCurrency,
        showsSats: Boolean,
    ): Long? {
        val fractionDigits = currency.getTypedFractionDigits(showsSats)
        val cleaned = text.filterNot { it.isWhitespace() }
        if (cleaned.isEmpty() || cleaned.any { !it.isDigit() && it !in SEPARATORS }) return null
        val lastSeparator = cleaned.indexOfLast { it in SEPARATORS }
        val decimals = if (lastSeparator < 0) "" else cleaned.substring(lastSeparator + 1)
        val isDecimal = lastSeparator >= 0 &&
            fractionDigits > 0 &&
            decimals.length <= fractionDigits &&
            cleaned.count { it == cleaned[lastSeparator] } == 1
        val wholeText = if (isDecimal) cleaned.substring(0, lastSeparator) else cleaned
        val whole = wholeText.filter { it.isDigit() }
        val fraction = if (isDecimal) decimals else ""
        if (whole.isEmpty() && fraction.isEmpty()) return null
        // Why: more digits than any amount the rules allow can't be a Long, nor a valid bill.
        if (whole.trimStart('0').length + fractionDigits > MAX_AMOUNT_DIGITS) return null
        val wholeMinor = (whole.ifEmpty { "0" }.toLong()) * factorOf(fractionDigits)
        val fractionMinor = fraction.padEnd(fractionDigits, '0').ifEmpty { "0" }.toLong()
        return wholeMinor + fractionMinor
    }

    /*
     * Why: an amount as an amount field shows it, "1.234,56" with the separator of the device's language and the
     * other one grouping thousands, which parse reads back.
     */
    fun format(
        amountMinor: Long,
        currency: GroupCurrency,
        showsSats: Boolean,
        decimalSeparator: Char,
    ): String {
        if (amountMinor < 0) return "-" + format(-amountMinor, currency, showsSats, decimalSeparator)
        val fractionDigits = currency.getTypedFractionDigits(showsSats)
        val factor = factorOf(fractionDigits)
        val whole = (amountMinor / factor)
            .toString()
            .reversed()
            .chunked(THOUSANDS)
            .joinToString(groupingSeparatorOf(decimalSeparator).toString())
            .reversed()
        if (fractionDigits == 0) return whole
        val fraction = (amountMinor % factor).toString().padStart(fractionDigits, '0')
        return whole + decimalSeparator + fraction
    }

    // Why: a whole number typed in a field of percentages or parts.
    fun parseWhole(text: String): Long? = text
        .filterNot { it.isWhitespace() }
        .takeIf { it.isNotEmpty() && it.length <= MAX_DIGITS && it.all(Char::isDigit) }
        ?.toLong()

    private fun factorOf(fractionDigits: Int): Long = (1..fractionDigits).fold(1L) { factor, _ -> factor * 10 }

    private fun groupingSeparatorOf(decimalSeparator: Char): Char = if (decimalSeparator == ',') '.' else ','

    private val SEPARATORS = setOf(',', '.')

    private const val MAX_DIGITS = 12

    // Why: the most digits of the minor unit a Long always holds, which every cap (21 million bitcoin in sats) fits.
    private const val MAX_AMOUNT_DIGITS = 18
    private const val THOUSANDS = 3
}
