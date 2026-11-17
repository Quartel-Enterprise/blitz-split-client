package com.quare.blitzsplit.android.designsystem.format

import android.icu.number.Notation
import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.model.GroupCurrency
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

// Why: Preferências' "Bitcoin em satoshis", which every bitcoin amount drawn follows, provided at the app's root.
val LocalShowsBitcoinInSats = staticCompositionLocalOf { false }

/*
 * Why: the shared code keeps amounts as a whole number of the currency's minor unit; the platform writes them in the
 * UI's language and the group's currency ("R$ 312,50" in Portuguese, "R$312.50" in English), with no decimals when
 * they are all zero ("R$ 312", `GroupCurrency.getDisplayFractionDigits`). Bitcoin and ether, which the platform has
 * no currency for, take ₿ and Ξ where the language puts a symbol, and only the decimals the amount needs ("₿ 0,00125");
 * bitcoin in satoshis is whole sats ("125.000 sats").
 */
@Stable
class MoneyFormatter(
    private val currency: GroupCurrency,
    private val showsSats: Boolean,
    // Why: "%1$s sats" and "sats", in the UI's language, from the composition that builds it.
    private val satsText: SatsText,
    locale: Locale,
) {
    // Why: 0-9 in every language (Arabic's own digits would not parse back in the amount fields).
    private val formatLocale: Locale = locale.withLatinDigits()

    // Why: each amount isolated from a right to left sentence around it; a left to right language's text is untouched.
    private val isolates: Boolean = locale.isRightToLeft()

    // Why: bitcoin, ether and a code this version doesn't know have no platform currency: the symbol is written by hand.
    private val writesOwnSymbol: Boolean = currency.isCrypto || !currency.isKnown

    private val isSats: Boolean = currency == GroupCurrency.BTC && showsSats

    // Why: lists format every row on each recomposition, and building a NumberFormat each time is costly.
    private val formats = mutableMapOf<Int, NumberFormat>()

    private val plainFormats = mutableMapOf<Int, NumberFormat>()

    private val bareSymbol: String = when {
        isSats -> satsText.symbol

        currency == GroupCurrency.BTC -> BITCOIN_SYMBOL

        currency == GroupCurrency.ETH -> ETHER_SYMBOL

        // Why: a code this version doesn't know has no symbol: the raw code stands in its place ("XYZ 12,50").
        !currency.isKnown -> currency.code

        else -> Currency.getInstance(currency.code).getSymbol(formatLocale)
    }

    // Why: the symbol a field shows before what is typed in it, whole in a right to left language ("US$", not "$US").
    val symbol: String = bareSymbol.toIsolatedIf(isolates)

    // Why: "0,00", the placeholder of an empty amount field, with the separator people type.
    val zeroPlaceholder: String = currency.getTypedFractionDigits(showsSats).let { digits ->
        val separator = DecimalFormatSymbols.getInstance(formatLocale).decimalSeparator
        if (digits == 0) "0" else "0" + separator + "0".repeat(digits)
    }

    fun toText(amountMinor: Long): String = toBareText(amountMinor).toIsolatedIf(isolates)

    private fun toBareText(amountMinor: Long): String {
        if (isSats) return satsText.amount.format(toBareAmountText(amountMinor))
        val digits = currency.getDisplayFractionDigits(amountMinor, showsSats)
        return formats.getOrPut(digits) { createCurrencyFormat(digits) }.format(amountMinor.toDecimal())
    }

    // Why: lazily, since only a chart's value lines write short amounts.
    private val compactFormat by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !currency.isCrypto && currency.isKnown) {
            NumberFormatter
                .withLocale(formatLocale)
                .notation(Notation.compactShort())
                .unit(
                    android.icu.util.Currency
                        .getInstance(currency.code),
                ).precision(Precision.maxSignificantDigits(COMPACT_DIGITS))
        } else {
            null
        }
    }

    /*
     * Why: "R$ 1,5 mil" or "R$1.5K", the short amount of a chart's value line. Bitcoin, ether and Android before 11,
     * which has no short currency format, write the whole amount.
     */
    fun toCompactText(amountMinor: Long): String {
        val format = compactFormat
        // Why: compactFormat is only set from Android 11, but lint can't see that through lazy, only this check.
        if (format == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return toText(amountMinor)
        val formatted = format.format(amountMinor.toDecimal())
        val text = formatted.toString()
        if (!isolates) return text
        // Why: the symbol in an isolate of its own in a right to left language, as toText's ("US$", never "$US").
        val characters = formatted.toCharacterIterator()
        val currencyField = android.icu.text.NumberFormat.Field.CURRENCY
        val start = (characters.beginIndex until characters.endIndex).firstOrNull { index ->
            characters.setIndex(index)
            characters.getAttribute(currencyField) != null
        }
        val symbolText = if (start == null) {
            text
        } else {
            val end = characters.getRunLimit(currencyField)
            text.substring(0, start) + text.substring(start, end).toIsolatedIf(true) + text.substring(end)
        }
        return symbolText.toIsolatedIf(true)
    }

    // Why: "120,50" with no symbol, where the currency is already on the same line ("você recebe 120 e deve 218,50").
    fun toAmountText(amountMinor: Long): String = toBareAmountText(amountMinor).toIsolatedIf(isolates)

    private fun toBareAmountText(amountMinor: Long): String {
        val digits = currency.getDisplayFractionDigits(amountMinor, showsSats)
        return plainFormats.getOrPut(digits) { createPlainFormat(digits) }.format(amountMinor.toDecimal())
    }

    // Why: "+R$ 600" and "−R$ 118,50", with the typographic minus of the design, the sign inside the amount's isolate.
    fun toSignedText(amountMinor: Long): String = when {
        amountMinor > 0 -> toBareText(amountMinor).withSign(PLUS_SIGN)
        amountMinor < 0 -> toBareText(-amountMinor).withSign(MINUS_SIGN)
        else -> toBareText(amountMinor)
    }.toIsolatedIf(isolates)

    /*
     * Why: the sign goes before the amount, but right before its digits in a right to left language, whose pattern
     * starts with a right to left mark ("‏141.66 R$") that would part a sign put first from the number it signs.
     */
    private fun String.withSign(sign: String): String {
        val digit = indexOfFirst(Char::isDigit)
        return if (!isolates || digit < 0) sign + this else substring(0, digit) + sign + substring(digit)
    }

    private fun Long.toDecimal(): BigDecimal = BigDecimal.valueOf(this, if (isSats) 0 else currency.fractionDigits)

    private fun createCurrencyFormat(fractionDigits: Int): NumberFormat =
        NumberFormat.getCurrencyInstance(formatLocale).apply {
            if (!writesOwnSymbol) currency = Currency.getInstance(this@MoneyFormatter.currency.code)
            /*
             * Why: bitcoin's, ether's or a raw code's symbol written by hand; and in a right to left language every
             * symbol in an isolate of its own, since Arabic writes "0 US$" and the "$" would drift to "$US 0".
             */
            if ((writesOwnSymbol || isolates) && this is DecimalFormat) {
                decimalFormatSymbols = decimalFormatSymbols.apply {
                    currencySymbol = symbol
                }
            }
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }

    private fun createPlainFormat(fractionDigits: Int): NumberFormat =
        NumberFormat.getNumberInstance(formatLocale).apply {
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }

    private companion object {
        const val MINUS_SIGN = "−"
        const val PLUS_SIGN = "+"
        const val BITCOIN_SYMBOL = "₿"
        const val ETHER_SYMBOL = "Ξ"
        const val COMPACT_DIGITS = 2
    }
}

@Composable
fun rememberSatsText(): SatsText {
    val amount = stringResource(R.string.designsystem_money_sats)
    val symbol = stringResource(R.string.designsystem_money_sats_symbol)
    return remember(amount, symbol) { SatsText(amount, symbol) }
}

@Composable
fun rememberMoneyFormatter(currency: GroupCurrency): MoneyFormatter {
    val locale = LocalLocale.current.platformLocale
    val showsSats = LocalShowsBitcoinInSats.current
    // Why: the root's cache first, so a row of a long list reads no string resource to find its formatter.
    val cache = LocalFormatterCache.current
    if (cache != null && cache.isFor(locale, showsSats)) return cache.moneyOf(currency)
    val satsText = rememberSatsText()
    return remember(currency, locale, showsSats, satsText) {
        MoneyFormatter(
            currency = currency,
            locale = locale,
            showsSats = showsSats,
            satsText = satsText,
        )
    }
}

// Why: one formatter per currency of a list (Saldo geral's totals), all following bitcoin's unit and the language.
@Composable
fun rememberMoneyFormatters(currencies: List<GroupCurrency>): Map<GroupCurrency, MoneyFormatter> {
    val locale = LocalLocale.current.platformLocale
    val showsSats = LocalShowsBitcoinInSats.current
    val satsText = rememberSatsText()
    return remember(currencies, locale, showsSats, satsText) {
        currencies.associateWith { currency ->
            MoneyFormatter(
                currency = currency,
                showsSats = showsSats,
                satsText = satsText,
                locale = locale,
            )
        }
    }
}
