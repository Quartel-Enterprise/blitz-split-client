package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import com.quare.blitzsplit.core.model.AppLanguage

private const val HINDI = "hi"
private const val THAI = "th"
private val unspacedLanguages = setOf(
    AppLanguage.JAPANESE,
    AppLanguage.CHINESE_SIMPLIFIED,
    AppLanguage.CHINESE_TRADITIONAL,
)

/*
 * Why: a sentence the code joins from pieces ("Ana" + "changed the category", two sentences of a detail) puts a space
 * between them, but Japanese and Chinese write them with none ("Anaがカテゴリを変更しました") and end a sentence with
 * "。", not ".". Hindi ends it with its danda, "।", and Thai with nothing, the space between two being its stop. Read
 * through `AppLanguage`, so both Chinese, whichever tag the device names them by. Same rule
 * as the web's `wordGap` and `fullStop`.
 */
@Composable
fun wordGapUi(): String = if (isUnspacedUi()) "" else " "

@Composable
fun fullStopUi(): String {
    if (isUnspacedUi()) return "。"
    return when (LocalLocale.current.platformLocale.language) {
        HINDI -> "।"
        THAI -> ""
        else -> "."
    }
}

@Composable
private fun isUnspacedUi(): Boolean =
    AppLanguage.fromTag(LocalLocale.current.platformLocale.toLanguageTag()) in unspacedLanguages
