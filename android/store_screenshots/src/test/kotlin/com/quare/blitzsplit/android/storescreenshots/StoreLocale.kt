package com.quare.blitzsplit.android.storescreenshots

/**
 * A language of the listings. [tag] names the generated folder; [language] picks the sample trip and the banner copy:
 * the app language's code, Portugal's Portuguese by its whole tag (pt-PT), since "pt" is Brazil's, and Spain's Spanish
 * by its own (es-ES), since "es" is Latin America's. [playLocale] is what Robolectric draws in, the Play screens and
 * the banner over an Apple capture alike, so it is always a language with at most a region, never a script: the
 * resources' language and the region the amounts, dates and hours are written in, the tag itself for every set but
 * Latin America's Spanish, Chinese, drawn in zh-CN for mainland China's yuan, Hebrew, drawn in iw-IL, the legacy
 * code Android's values-iw answers to, Thai, drawn in th-TH for the baht, Vietnamese, drawn in vi-VN for the
 * đồng, Traditional Chinese, drawn in zh-TW for the New Taiwan dollar, Swedish, drawn in sv-SE for the krona, and
 * Czech, drawn in cs-CZ for the koruna. The stores' own codes (es-419, es-MX…) come from the tags in the staging tasks
 * of build-logic's StoreScreenshots.kt.
 */
internal data class StoreLocale(
    val tag: String,
    val language: String,
    val playLocale: String,
)

/**
 * The region Latin America's Spanish set is drawn in on Play: Equatorial Guinea's, the one its App Store set is
 * captured in (the `es` line of scripts/store-screenshots/locales.tsv), so one listing writes "€84,08", "US$120" and
 * "21:00" on both stores. A bare "es" writes the symbol after the amount, as Spain does ("84,08 €", "120 US$"). The
 * strings stay Latin America's: es-GQ reads values-es, never values-es-rES. If the iOS line changes region, change
 * this with it (docs/store-listing-screenshots.md).
 */
internal const val LATIN_AMERICAN_PLAY_LOCALE = "es-GQ"

internal val storeLocales = listOf(
    StoreLocale(
        tag = "en-US",
        language = "en",
        playLocale = "en-US",
    ),
    StoreLocale(
        tag = "pt-BR",
        language = "pt",
        playLocale = "pt-BR",
    ),
    StoreLocale(
        tag = "pt-PT",
        language = "pt-PT",
        playLocale = "pt-PT",
    ),
    StoreLocale(
        tag = "es",
        language = "es",
        playLocale = LATIN_AMERICAN_PLAY_LOCALE,
    ),
    StoreLocale(
        tag = "es-ES",
        language = "es-ES",
        playLocale = "es-ES",
    ),
    StoreLocale(
        tag = "fr",
        language = "fr",
        playLocale = "fr",
    ),
    StoreLocale(
        tag = "de",
        language = "de",
        playLocale = "de",
    ),
    StoreLocale(
        tag = "it",
        language = "it",
        playLocale = "it",
    ),
    StoreLocale(
        tag = "pl",
        language = "pl",
        playLocale = "pl",
    ),
    StoreLocale(
        tag = "nl",
        language = "nl",
        playLocale = "nl",
    ),
    StoreLocale(
        tag = "tr",
        language = "tr",
        playLocale = "tr",
    ),
    StoreLocale(
        tag = "id",
        language = "id",
        playLocale = "id",
    ),
    StoreLocale(
        tag = "ru",
        language = "ru",
        playLocale = "ru",
    ),
    StoreLocale(
        tag = "uk",
        language = "uk",
        playLocale = "uk",
    ),
    StoreLocale(
        tag = "ko",
        language = "ko",
        playLocale = "ko",
    ),
    StoreLocale(
        tag = "hi",
        language = "hi",
        playLocale = "hi",
    ),
    StoreLocale(
        tag = "ar",
        language = "ar",
        playLocale = "ar",
    ),
    StoreLocale(
        tag = "zh",
        language = "zh",
        playLocale = "zh-CN",
    ),
    StoreLocale(
        tag = "he",
        language = "he",
        playLocale = "iw-IL",
    ),
    StoreLocale(
        tag = "th",
        language = "th",
        playLocale = "th-TH",
    ),
    StoreLocale(
        tag = "vi",
        language = "vi",
        playLocale = "vi-VN",
    ),
    StoreLocale(
        tag = "zh-Hant",
        language = "zh-Hant",
        playLocale = "zh-TW",
    ),
    StoreLocale(
        tag = "sv",
        language = "sv",
        playLocale = "sv-SE",
    ),
    StoreLocale(
        tag = "cs",
        language = "cs",
        playLocale = "cs-CZ",
    ),
    StoreLocale(
        tag = "ro",
        language = "ro",
        playLocale = "ro",
    ),
    StoreLocale(
        tag = "ja",
        language = "ja",
        playLocale = "ja",
    ),
)
