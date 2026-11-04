package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLocale
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
    private val locale: Locale,
    private val showsSats: Boolean = false,
) {
    private val isSats: Boolean = currency == GroupCurrency.BTC && showsSats

    // Why: lists format every row on each recomposition, and building a NumberFormat each time is costly.
    private val formats = mutableMapOf<Int, NumberFormat>()

    private val plainFormats = mutableMapOf<Int, NumberFormat>()

    // Why: the symbol a field shows before what is typed in it.
    val symbol: String = when {
        isSats -> SATS
        currency == GroupCurrency.BTC -> BITCOIN_SYMBOL
        currency == GroupCurrency.ETH -> ETHER_SYMBOL
        else -> Currency.getInstance(currency.code).getSymbol(locale)
    }

    // Why: "0,00", the placeholder of an empty amount field, with the separator people type.
    val zeroPlaceholder: String = currency.getTypedFractionDigits(showsSats).let { digits ->
        if (digits == 0) "0" else "0" + DecimalFormatSymbols.getInstance(locale).decimalSeparator + "0".repeat(digits)
    }

    fun toText(amountMinor: Long): String {
        if (isSats) return "${toAmountText(amountMinor)} $SATS"
        val digits = currency.getDisplayFractionDigits(amountMinor, showsSats)
        return formats.getOrPut(digits) { createCurrencyFormat(digits) }.format(amountMinor.toDecimal())
    }

    // Why: "120,50" with no symbol, where the currency is already on the same line ("você recebe 120 e deve 218,50").
    fun toAmountText(amountMinor: Long): String {
        val digits = currency.getDisplayFractionDigits(amountMinor, showsSats)
        return plainFormats.getOrPut(digits) { createPlainFormat(digits) }.format(amountMinor.toDecimal())
    }

    // Why: "+R$ 600" and "−R$ 118,50", with the typographic minus of the design.
    fun toSignedText(amountMinor: Long): String = when {
        amountMinor > 0 -> PLUS_SIGN + toText(amountMinor)
        amountMinor < 0 -> MINUS_SIGN + toText(-amountMinor)
        else -> toText(amountMinor)
    }

    private fun Long.toDecimal(): BigDecimal = BigDecimal.valueOf(this, if (isSats) 0 else currency.fractionDigits)

    private fun createCurrencyFormat(fractionDigits: Int): NumberFormat =
        NumberFormat.getCurrencyInstance(locale).apply {
            if (this@MoneyFormatter.currency.isCrypto && this is DecimalFormat) {
                decimalFormatSymbols = decimalFormatSymbols.apply {
                    currencySymbol = symbol
                }
            } else {
                currency = Currency.getInstance(this@MoneyFormatter.currency.code)
            }
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }

    private fun createPlainFormat(fractionDigits: Int): NumberFormat = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }

    private companion object {
        const val MINUS_SIGN = "−"
        const val PLUS_SIGN = "+"
        const val BITCOIN_SYMBOL = "₿"
        const val ETHER_SYMBOL = "Ξ"
        const val SATS = "sats"
    }
}

@Composable
fun rememberMoneyFormatter(currency: GroupCurrency): MoneyFormatter {
    val locale = LocalLocale.current.platformLocale
    val showsSats = LocalShowsBitcoinInSats.current
    return remember(currency, locale, showsSats) {
        MoneyFormatter(
            currency = currency,
            locale = locale,
            showsSats = showsSats,
        )
    }
}
