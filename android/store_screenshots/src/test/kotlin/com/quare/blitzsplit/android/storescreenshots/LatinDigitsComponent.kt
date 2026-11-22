package com.quare.blitzsplit.android.storescreenshots

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.quare.blitzsplit.android.designsystem.format.withLatinDigits

/*
 * Why: the app writes its counts with 0-9 through LegacyAppLocale.wrap, which Robolectric never runs, so a "%d" of a
 * string resource would be drawn in Bengali's own digits here.
 */
@Composable
internal fun LatinDigitsComponent(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val latinConfiguration = remember(configuration) {
        Configuration(configuration).apply { setLocale(configuration.locales[0].withLatinDigits()) }
    }
    val latinContext = remember(context, latinConfiguration) { context.createConfigurationContext(latinConfiguration) }
    CompositionLocalProvider(
        LocalContext provides latinContext,
        LocalConfiguration provides latinConfiguration,
        content = content,
    )
}
