package com.quare.blitzsplit.android.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.TextUnit
import java.util.Currency
import java.util.Locale

private const val DEFAULT_CURRENCY_SYMBOL = "$"
private const val LONG_SYMBOL_LENGTH = 3
private const val LONG_SYMBOL_SCALE = 0.7f

// Why: the brand badge shows the currency people split bills in, the one of the device's region.
@Composable
fun rememberCurrencySymbol(): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { locale.toCurrencySymbol() }
}

// Why: symbols like "CHF" or "kr" would overflow a badge sized for "R$".
fun toCurrencyFontSize(
    symbol: String,
    baseSize: TextUnit,
): TextUnit = if (symbol.length >= LONG_SYMBOL_LENGTH) baseSize * LONG_SYMBOL_SCALE else baseSize

// Why: a locale without a region (or a region without a currency) has no symbol to show.
private fun Locale.toCurrencySymbol(): String = try {
    Currency.getInstance(this)?.getSymbol(this) ?: DEFAULT_CURRENCY_SYMBOL
} catch (noRegion: IllegalArgumentException) {
    DEFAULT_CURRENCY_SYMBOL
}
