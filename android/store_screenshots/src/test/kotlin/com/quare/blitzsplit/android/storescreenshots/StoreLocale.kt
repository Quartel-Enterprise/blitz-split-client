package com.quare.blitzsplit.android.storescreenshots

/**
 * A language of the listings. [tag] names the generated folder and is what Robolectric switches the resources to;
 * [language] picks the sample trip and the banner copy. The stores' own codes (es-419, es-MX…) come from these in
 * the staging tasks of the root build.gradle.kts.
 */
internal data class StoreLocale(
    val tag: String,
    val language: String,
)

internal val storeLocales = listOf(
    StoreLocale(tag = "en-US", language = "en"),
    StoreLocale(tag = "pt-BR", language = "pt"),
    StoreLocale(tag = "es", language = "es"),
    StoreLocale(tag = "fr", language = "fr"),
    StoreLocale(tag = "de", language = "de"),
    StoreLocale(tag = "it", language = "it"),
    StoreLocale(tag = "pl", language = "pl"),
    StoreLocale(tag = "nl", language = "nl"),
    StoreLocale(tag = "tr", language = "tr"),
    StoreLocale(tag = "id", language = "id"),
    StoreLocale(tag = "ja", language = "ja"),
)
