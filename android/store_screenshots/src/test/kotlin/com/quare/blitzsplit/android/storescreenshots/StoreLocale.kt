package com.quare.blitzsplit.android.storescreenshots

/**
 * A language of the listings. [tag] names the generated folder and is what Robolectric switches the resources to;
 * [language] picks the sample trip and the banner copy: the app language's code, Portugal's Portuguese by its whole
 * tag (pt-PT), since "pt" is Brazil's, and Spain's Spanish by its own (es-ES), since "es" is Latin America's. The
 * stores' own codes (es-419, es-MX…) come from these in the staging tasks of build-logic's StoreScreenshots.kt.
 */
internal data class StoreLocale(
    val tag: String,
    val language: String,
)

internal val storeLocales = listOf(
    StoreLocale(
        tag = "en-US",
        language = "en",
    ),
    StoreLocale(
        tag = "pt-BR",
        language = "pt",
    ),
    StoreLocale(
        tag = "pt-PT",
        language = "pt-PT",
    ),
    StoreLocale(
        tag = "es",
        language = "es",
    ),
    StoreLocale(
        tag = "es-ES",
        language = "es-ES",
    ),
    StoreLocale(
        tag = "fr",
        language = "fr",
    ),
    StoreLocale(
        tag = "de",
        language = "de",
    ),
    StoreLocale(
        tag = "it",
        language = "it",
    ),
    StoreLocale(
        tag = "pl",
        language = "pl",
    ),
    StoreLocale(
        tag = "nl",
        language = "nl",
    ),
    StoreLocale(
        tag = "tr",
        language = "tr",
    ),
    StoreLocale(
        tag = "id",
        language = "id",
    ),
    StoreLocale(
        tag = "ru",
        language = "ru",
    ),
    StoreLocale(
        tag = "uk",
        language = "uk",
    ),
    StoreLocale(
        tag = "ja",
        language = "ja",
    ),
)
