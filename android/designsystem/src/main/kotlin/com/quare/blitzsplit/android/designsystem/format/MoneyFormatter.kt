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
 * UI's language and the group's currency ("R$ 312,00" in Portuguese, "R$312.00" in English).
 */
@Stable
class MoneyFormatter(
    currency: GroupCurrency,
    locale: Locale,
) {
    private val fractionDigits = currency.fractionDigits

    private val format: NumberFormat = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = Currency.getInstance(currency.code)
        minimumFractionDigits = currency.fractionDigits
        maximumFractionDigits = currency.fractionDigits
    }

    // Why: the symbol a field shows before what is typed in it.
    val symbol: String = Currency.getInstance(currency.code).getSymbol(locale)

    // Why: "0,00", the placeholder of an empty amount field, with the separator people type.
    val zeroPlaceholder: String = if (currency.fractionDigits == 0) {
        "0"
    } else {
        "0" + DecimalFormatSymbols.getInstance(locale).decimalSeparator + "0".repeat(currency.fractionDigits)
    }

    fun toText(amountMinor: Long): String = format.format(BigDecimal.valueOf(amountMinor, fractionDigits))

    // Why: "+R$ 600,00" and "−R$ 118,00", with the typographic minus of the design.
    fun toSignedText(amountMinor: Long): String = when {
        amountMinor > 0 -> PLUS_SIGN + toText(amountMinor)
        amountMinor < 0 -> MINUS_SIGN + toText(-amountMinor)
        else -> toText(amountMinor)
    }

    private companion object {
        const val MINUS_SIGN = "\u2212"
        const val PLUS_SIGN = "+"
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
