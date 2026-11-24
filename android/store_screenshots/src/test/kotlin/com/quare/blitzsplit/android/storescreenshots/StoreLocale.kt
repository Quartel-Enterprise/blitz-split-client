package com.quare.blitzsplit.android.storescreenshots

import androidx.compose.ui.unit.LayoutDirection
import com.quare.blitzsplit.android.designsystem.format.isRightToLeft
import java.util.Locale

/**
 * A language of the listings. [tag] names the generated folder; [language] picks the sample trip:
 * the app language's code, Portugal's Portuguese by its whole tag (pt-PT), since "pt" is Brazil's, and Spain's Spanish
 * by its own (es-ES), since "es" is Latin America's, and Britain's, Australia's and Canada's English by theirs (en-GB,
 * en-AU, en-CA), since "en" is the American one. [drawnLocale] is what Robolectric draws the Play screens in, so it is a language with at most a region, and a script only
 * where the language is written in two and the app has one of them (Serbian's Latin): the
 * resources' language and the region the amounts, dates and hours are written in, the tag itself for every set but
 * Latin America's Spanish, Chinese, drawn in zh-CN for mainland China's yuan, Hebrew, drawn in iw-IL, the legacy
 * code Android's values-iw answers to, Thai, drawn in th-TH for the baht, Vietnamese, drawn in vi-VN for the
 * đồng, Traditional Chinese, drawn in zh-TW for the New Taiwan dollar, Swedish, drawn in sv-SE for the krona,
 * Czech, drawn in cs-CZ for the koruna, Bengali, drawn in bn-BD for the taka, Malay, drawn in ms-MY for the
 * ringgit, Hungarian, drawn in hu-HU for the forint, Urdu, drawn in ur-PK for the rupee, Filipino, drawn in fil-PH for the peso, Serbian, drawn in sr-Latn-RS for the dinar, Persian, drawn in fa-IR for the rial, Norwegian, drawn in nb-NO for the krone, Swahili, drawn in sw-KE for the Kenyan shilling, Catalan, drawn in ca-ES for Spain's euros (its tag having no region), Danish, drawn in da-DK for the krone, Finnish, drawn in fi-FI for the euro, Latvian, drawn in lv-LV for the euro, Estonian, drawn in et-EE for the euro, and Slovenian, drawn in sl-SI for the euro. The stores' own
 * codes (es-419, es-MX…) come from the tags in scripts/release/store-listing.mjs.
 */
internal data class StoreLocale(
    val tag: String,
    val language: String,
    val drawnLocale: String,
)

/**
 * The way the set's language is read: right to left for Arabic, Hebrew, Urdu and Persian, by the app's own rule, the one its
 * arrows turn by. Robolectric draws every locale left to right unless told otherwise, so a Play screen is handed this
 * (PlayArtCaptures).
 */
internal val StoreLocale.layoutDirection: LayoutDirection
    get() = if (Locale.forLanguageTag(drawnLocale).isRightToLeft()) LayoutDirection.Rtl else LayoutDirection.Ltr

/**
 * The locale Latin America's Spanish set is drawn in: Equatorial Guinea's region, the one its App Store set is
 * captured in (the `es` line of scripts/store-screenshots/locales.tsv), so one listing writes "€84,08", "US$120" and
 * "21:00" on both stores. A bare "es" writes the symbol after the amount, as Spain does ("84,08 €", "120 US$"). The
 * strings stay Latin America's: es-GQ reads values-es, never values-es-rES. If the iOS line changes region, change
 * this with it (docs/store-listing-screenshots.md).
 */
internal const val LATIN_AMERICAN_DRAWN_LOCALE = "es-GQ"

internal val storeLocales = listOf(
    StoreLocale(
        tag = "en-US",
        language = "en",
        drawnLocale = "en-US",
    ),
    StoreLocale(
        tag = "en-GB",
        language = "en-GB",
        drawnLocale = "en-GB",
    ),
    StoreLocale(
        tag = "en-AU",
        language = "en-AU",
        drawnLocale = "en-AU",
    ),
    StoreLocale(
        tag = "en-CA",
        language = "en-CA",
        drawnLocale = "en-CA",
    ),
    StoreLocale(
        tag = "pt-BR",
        language = "pt",
        drawnLocale = "pt-BR",
    ),
    StoreLocale(
        tag = "pt-PT",
        language = "pt-PT",
        drawnLocale = "pt-PT",
    ),
    StoreLocale(
        tag = "es",
        language = "es",
        drawnLocale = LATIN_AMERICAN_DRAWN_LOCALE,
    ),
    StoreLocale(
        tag = "es-ES",
        language = "es-ES",
        drawnLocale = "es-ES",
    ),
    StoreLocale(
        tag = "fr",
        language = "fr",
        drawnLocale = "fr",
    ),
    StoreLocale(
        tag = "de",
        language = "de",
        drawnLocale = "de",
    ),
    StoreLocale(
        tag = "it",
        language = "it",
        drawnLocale = "it",
    ),
    StoreLocale(
        tag = "pl",
        language = "pl",
        drawnLocale = "pl",
    ),
    StoreLocale(
        tag = "nl",
        language = "nl",
        drawnLocale = "nl",
    ),
    StoreLocale(
        tag = "tr",
        language = "tr",
        drawnLocale = "tr",
    ),
    StoreLocale(
        tag = "id",
        language = "id",
        drawnLocale = "id",
    ),
    StoreLocale(
        tag = "ru",
        language = "ru",
        drawnLocale = "ru",
    ),
    StoreLocale(
        tag = "uk",
        language = "uk",
        drawnLocale = "uk",
    ),
    StoreLocale(
        tag = "ko",
        language = "ko",
        drawnLocale = "ko",
    ),
    StoreLocale(
        tag = "hi",
        language = "hi",
        drawnLocale = "hi",
    ),
    StoreLocale(
        tag = "ar",
        language = "ar",
        drawnLocale = "ar",
    ),
    StoreLocale(
        tag = "zh",
        language = "zh",
        drawnLocale = "zh-CN",
    ),
    StoreLocale(
        tag = "he",
        language = "he",
        drawnLocale = "iw-IL",
    ),
    StoreLocale(
        tag = "th",
        language = "th",
        drawnLocale = "th-TH",
    ),
    StoreLocale(
        tag = "vi",
        language = "vi",
        drawnLocale = "vi-VN",
    ),
    StoreLocale(
        tag = "zh-Hant",
        language = "zh-Hant",
        drawnLocale = "zh-TW",
    ),
    StoreLocale(
        tag = "sv",
        language = "sv",
        drawnLocale = "sv-SE",
    ),
    StoreLocale(
        tag = "cs",
        language = "cs",
        drawnLocale = "cs-CZ",
    ),
    StoreLocale(
        tag = "ro",
        language = "ro",
        drawnLocale = "ro",
    ),
    StoreLocale(
        tag = "bn",
        language = "bn",
        drawnLocale = "bn-BD",
    ),
    StoreLocale(
        tag = "ms",
        language = "ms",
        drawnLocale = "ms-MY",
    ),
    StoreLocale(
        tag = "el",
        language = "el",
        drawnLocale = "el",
    ),
    StoreLocale(
        tag = "hr",
        language = "hr",
        drawnLocale = "hr",
    ),
    StoreLocale(
        tag = "hu",
        language = "hu",
        drawnLocale = "hu-HU",
    ),
    StoreLocale(
        tag = "ur",
        language = "ur",
        drawnLocale = "ur-PK",
    ),
    StoreLocale(
        tag = "fil",
        language = "fil",
        drawnLocale = "fil-PH",
    ),
    StoreLocale(
        tag = "sr",
        language = "sr",
        drawnLocale = "sr-Latn-RS",
    ),
    StoreLocale(
        tag = "sk",
        language = "sk",
        drawnLocale = "sk",
    ),
    StoreLocale(
        tag = "sw",
        language = "sw",
        drawnLocale = "sw-KE",
    ),
    StoreLocale(
        tag = "fa",
        language = "fa",
        drawnLocale = "fa-IR",
    ),
    StoreLocale(
        tag = "ta",
        language = "ta",
        drawnLocale = "ta",
    ),
    StoreLocale(
        tag = "nb",
        language = "nb",
        drawnLocale = "nb-NO",
    ),
    StoreLocale(
        tag = "te",
        language = "te",
        drawnLocale = "te",
    ),
    StoreLocale(
        tag = "bg",
        language = "bg",
        drawnLocale = "bg",
    ),
    StoreLocale(
        tag = "mr",
        language = "mr",
        drawnLocale = "mr",
    ),
    StoreLocale(
        tag = "ca",
        language = "ca",
        drawnLocale = "ca-ES",
    ),
    StoreLocale(
        tag = "da",
        language = "da",
        drawnLocale = "da-DK",
    ),
    StoreLocale(
        tag = "fi",
        language = "fi",
        drawnLocale = "fi-FI",
    ),
    StoreLocale(
        tag = "lv",
        language = "lv",
        drawnLocale = "lv-LV",
    ),
    StoreLocale(
        tag = "et",
        language = "et",
        drawnLocale = "et-EE",
    ),
    StoreLocale(
        tag = "gu",
        language = "gu",
        drawnLocale = "gu",
    ),
    StoreLocale(
        tag = "sl",
        language = "sl",
        drawnLocale = "sl-SI",
    ),
    StoreLocale(
        tag = "pa",
        language = "pa",
        drawnLocale = "pa",
    ),
    StoreLocale(
        tag = "ja",
        language = "ja",
        drawnLocale = "ja",
    ),
)
