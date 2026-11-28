package com.quare.blitzsplit.android.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import com.quare.blitzsplit.android.designsystem.R
import com.quare.blitzsplit.core.model.AppLanguage

private const val HINDI = "hi"
private const val BENGALI = "bn"
private const val ODIA = "or"
private const val URDU = "ur"
private const val THAI = "th"
private const val BURMESE = "my"
private const val KHMER = "km"
private val unspacedLanguages = setOf(
    AppLanguage.JAPANESE,
    AppLanguage.CHINESE_SIMPLIFIED,
    AppLanguage.CHINESE_TRADITIONAL,
)

/*
 * Why: a sentence the code joins from pieces ("Ana" + "changed the category", two sentences of a detail) puts a space
 * between them, but Japanese and Chinese write them with none ("Anaがカテゴリを変更しました") and end a sentence with
 * "。", not ".". Hindi, Bengali, Odia and Punjabi end it with their danda, "।" (Punjabi told by the strings drawn,
 * `designsystem_punjabi`, since a Shahmukhi device draws English's), Urdu with "۔", Burmese with "။", Khmer with "។", and Thai with nothing, the
 * space between two being its stop. Read through `AppLanguage`, so both Chinese are unspaced whichever tag the device names them by
 * (zh-TW, zh-Hant-HK, zh-CN…). Same rule as the web's `wordGap` and `fullStop`.
 */
@Composable
fun wordGapUi(): String = if (isUnspacedUi()) "" else " "

@Composable
fun fullStopUi(): String {
    if (isUnspacedUi()) return "。"
    if (LocalContext.current.resources.getBoolean(R.bool.designsystem_punjabi)) return "।"
    return when (LocalLocale.current.platformLocale.language) {
        HINDI, BENGALI, ODIA -> "।"
        URDU -> "۔"
        BURMESE -> "။"
        KHMER -> "។"
        THAI -> ""
        else -> "."
    }
}

@Composable
private fun isUnspacedUi(): Boolean =
    AppLanguage.fromTag(LocalLocale.current.platformLocale.toLanguageTag()) in unspacedLanguages
