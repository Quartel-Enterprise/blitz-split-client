package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.IllformedLocaleException
import java.util.Locale

/*
 * Why: the right to left languages (Hebrew under its current code and the legacy one Java still hands out, "iw"),
 * and the ones whose own digits the app writes as 0-9 (Arabic; Persian and Urdu would join once the app speaks them).
 */
private val rightToLeftLanguages = setOf("ar", "fa", "he", "iw", "ur")
private val latinDigitLanguages = setOf("ar")
private const val FIRST_STRONG_ISOLATE = '\u2068'
private const val POP_DIRECTIONAL_ISOLATE = '\u2069'

fun Locale.isRightToLeft(): Boolean = language in rightToLeftLanguages

/*
 * Why: amounts, dates and counts are written with 0-9 in every language, so what a field shows is what it parses and
 * an amount reads the same as the one typed on the number keyboard. Arabic's own digits stay for its words only.
 */
fun Locale.withLatinDigits(): Locale {
    if (language !in latinDigitLanguages) return this
    return try {
        Locale
            .Builder()
            .setLocale(this)
            .setUnicodeLocaleKeyword("nu", "latn")
            .build()
    } catch (_: IllformedLocaleException) {
        this
    }
}

/*
 * Why: an amount set inside a right to left sentence ("دفعت ⁨R$ 12.50⁩ لـ Ana") keeps its symbol, sign and digits in
 * their own order; a left to right language gets the text untouched.
 */
internal fun String.toIsolatedIf(isRightToLeft: Boolean): String =
    if (isRightToLeft) "$FIRST_STRONG_ISOLATE$this$POP_DIRECTIONAL_ISOLATE" else this

/*
 * Why: the digits typed in an amount field run left to right ("12." never shows as ".12") while the field itself, its
 * symbol and label, sit on the right to left side; the text keeps the side it had (Start or End of the layout).
 * A left to right layout gets the style untouched.
 */
@Composable
fun TextStyle.withLeftToRightDigits(): TextStyle {
    if (LocalLayoutDirection.current != LayoutDirection.Rtl) return this
    val physicalAlign = when (textAlign) {
        TextAlign.End -> TextAlign.Left
        TextAlign.Center, TextAlign.Justify, TextAlign.Left, TextAlign.Right -> textAlign
        else -> TextAlign.Right
    }
    return copy(
        textDirection = TextDirection.Ltr,
        textAlign = physicalAlign,
    )
}

// Why: "Bar → Restaurante" points the way the line reads, so a right to left language gets "←", by the app's language as on iOS and the web.
@Composable
fun forwardArrowUi(): String = if (LocalLocale.current.platformLocale.isRightToLeft()) "←" else "→"
