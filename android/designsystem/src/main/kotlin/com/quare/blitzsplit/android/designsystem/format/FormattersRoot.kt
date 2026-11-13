package com.quare.blitzsplit.android.designsystem.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.quare.blitzsplit.core.model.GroupCurrency
import java.time.ZoneId
import java.util.Locale

internal val LocalFormatterCache = staticCompositionLocalOf<FormatterCache?> { null }

/*
 * Why: a row of a list asks for its currency's formatter and the dates' as it scrolls in, and building them (ICU's
 * patterns, a NumberFormat) once per row is what a long list pays for; the app's root keeps one of each for the
 * language, bitcoin's unit and the clock it draws in. A preview, which has no root, builds its own as before.
 */
internal class FormatterCache(
    private val locale: Locale,
    private val showsSats: Boolean,
    private val satsText: SatsText,
    zone: ZoneId,
    is24Hour: Boolean,
) {
    private val moneyByCurrency = mutableMapOf<GroupCurrency, MoneyFormatter>()

    val dates: DateTimeTextFormatter by lazy {
        DateTimeTextFormatter(
            locale = locale,
            zone = zone,
            is24Hour = is24Hour,
        )
    }

    fun moneyOf(currency: GroupCurrency): MoneyFormatter = moneyByCurrency.getOrPut(currency) {
        MoneyFormatter(
            currency = currency,
            locale = locale,
            showsSats = showsSats,
            satsText = satsText,
        )
    }

    // Why: a theme or a language nested under the root (a preview of another one) builds its own formatters.
    fun isFor(
        locale: Locale,
        showsSats: Boolean,
        satsText: SatsText,
    ): Boolean = locale == this.locale && showsSats == this.showsSats && satsText == this.satsText

    fun isFor(locale: Locale): Boolean = locale == this.locale
}

@Composable
fun FormattersRoot(content: @Composable () -> Unit) {
    val locale = LocalLocale.current.platformLocale
    val showsSats = LocalShowsBitcoinInSats.current
    val satsText = rememberSatsText()
    val context = LocalContext.current
    var is24Hour by remember { mutableStateOf(DateFormat.is24HourFormat(context)) }
    var zone by remember { mutableStateOf(ZoneId.systemDefault()) }
    /*
     * Why: the clock's 12 or 24 hours and the time zone change in the system's settings, while the app is away. The
     * cache is only built again when one of them really changed, so coming back from a picker redraws nothing.
     */
    LifecycleResumeEffect(context) {
        is24Hour = DateFormat.is24HourFormat(context)
        zone = ZoneId.systemDefault()
        onPauseOrDispose {}
    }
    val cache = remember(locale, showsSats, satsText, zone, is24Hour) {
        FormatterCache(
            locale = locale,
            showsSats = showsSats,
            satsText = satsText,
            zone = zone,
            is24Hour = is24Hour,
        )
    }
    CompositionLocalProvider(
        LocalFormatterCache provides cache,
        content = content,
    )
}
