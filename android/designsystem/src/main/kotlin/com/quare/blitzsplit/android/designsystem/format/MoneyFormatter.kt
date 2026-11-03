package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import com.quare.blitzsplit.core.model.GroupCurrency
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/*
 * Why: the shared code keeps amounts as a whole number of the currency's minor unit; the platform writes them in the
 * UI's language and the group's currency ("R$ 312,50" in Portuguese, "R$312.50" in English), with no decimals when
 * they are all zero ("R$ 312", `GroupCurrency.getDisplayFractionDigits`).
 */
@Stable
class MoneyFormatter(
    currency: GroupCurrency,
    locale: Locale,
) {
    private val groupCurrency = currency

    private val format: NumberFormat = createCurrencyFormat(currency, locale, currency.fractionDigits)

    private val wholeFormat: NumberFormat = createCurrencyFormat(currency, locale, fractionDigits = 0)

    // Why: the symbol a field shows before what is typed in it.
    val symbol: String = Currency.getInstance(currency.code).getSymbol(locale)

    // Why: "0,00", the placeholder of an empty amount field, with the separator people type.
    val zeroPlaceholder: String = if (currency.fractionDigits == 0) {
        "0"
    } else {
        "0" + DecimalFormatSymbols.getInstance(locale).decimalSeparator + "0".repeat(currency.fractionDigits)
    }

    private val plainFormat: NumberFormat = createPlainFormat(locale, currency.fractionDigits)

    private val wholePlainFormat: NumberFormat = createPlainFormat(locale, fractionDigits = 0)

    fun toText(amountMinor: Long): String = pick(amountMinor, format, wholeFormat).format(amountMinor.toDecimal())

    // Why: "120,50" with no symbol, where the currency is already on the same line ("você recebe 120 e deve 218,50").
    fun toAmountText(amountMinor: Long): String =
        pick(amountMinor, plainFormat, wholePlainFormat).format(amountMinor.toDecimal())

    // Why: "+R$ 600" and "−R$ 118,50", with the typographic minus of the design.
    fun toSignedText(amountMinor: Long): String = when {
        amountMinor > 0 -> PLUS_SIGN + toText(amountMinor)
        amountMinor < 0 -> MINUS_SIGN + toText(-amountMinor)
        else -> toText(amountMinor)
    }

    private fun pick(
        amountMinor: Long,
        withDecimals: NumberFormat,
        whole: NumberFormat,
    ): NumberFormat = if (groupCurrency.getDisplayFractionDigits(amountMinor) == 0) whole else withDecimals

    private fun Long.toDecimal(): BigDecimal = BigDecimal.valueOf(this, groupCurrency.fractionDigits)

    private companion object {
        const val MINUS_SIGN = "\u2212"
        const val PLUS_SIGN = "+"

        fun createCurrencyFormat(
            currency: GroupCurrency,
            locale: Locale,
            fractionDigits: Int,
        ): NumberFormat = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = Currency.getInstance(currency.code)
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }

        fun createPlainFormat(
            locale: Locale,
            fractionDigits: Int,
        ): NumberFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }
    }
}

@Composable
fun rememberMoneyFormatter(currency: GroupCurrency): MoneyFormatter {
    val locale = LocalLocale.current.platformLocale
    return remember(currency, locale) {
        MoneyFormatter(
            currency = currency,
            locale = locale,
        )
    }
}
