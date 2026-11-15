package com.quare.blitzsplit.core.model

/*
 * Why: Profile › Language, kept per device by each platform's own per-app language and never synced. SYSTEM follows the
 * device, English for a language the app doesn't have; the tag is what each platform hands its per-app language.
 */
enum class AppLanguage(
    val tag: String?,
) {
    SYSTEM(null),
    ENGLISH("en"),
    PORTUGUESE("pt-BR"),
    PORTUGUESE_PORTUGAL("pt-PT"),
    SPANISH("es"),
    FRENCH("fr"),
    GERMAN("de"),
    ITALIAN("it"),
    POLISH("pl"),
    DUTCH("nl"),
    JAPANESE("ja"),
    TURKISH("tr"),
    INDONESIAN("id"),
    ;

    /*
     * Why: what the language picker's chip shows ("EN", "ES", "BR"): the language's code, but Portuguese comes written
     * for two regions, so each shows its region and the two chips differ.
     */
    val chipCode: String?
        get() = if (this == PORTUGUESE || this == PORTUGUESE_PORTUGAL) {
            tag?.substringAfter('-')
        } else {
            tag?.substringBefore('-')
        }?.uppercase()

    // Why: Spain's Spanish (es-ES) only overrides the words Spain says differently, so a device in Spain keeps them.
    fun toTag(region: String?): String? = if (this == SPANISH &&
        region.equals("ES", ignoreCase = true)
    ) {
        "es-ES"
    } else {
        tag
    }

    companion object {
        // Why: Android still names Indonesian by its legacy code "in" (values-in), so a tag read from it may carry it.
        private const val LEGACY_INDONESIAN = "in"

        private const val PORTUGUESE_LANGUAGE = "pt"
        private const val PORTUGAL = "PT"

        /*
         * Why: any Portuguese, Spanish, French, German, Italian, Polish, Dutch, Japanese, Turkish, Indonesian or English
         * tag the platform kept (pt-BR, es-ES, fr-CA, de-AT, it-CH, pl-PL, nl-BE, ja-JP, tr-CY, id-ID, en-GB), SYSTEM for
         * none or any other. Portuguese is two entries: pt-PT is Portugal's, every other tag (pt, pt-BR…) Brazil's.
         */
        fun fromTag(tag: String?): AppLanguage {
            val language = languageOf(tag)
            if (language == PORTUGUESE_LANGUAGE) {
                return if (regionOf(tag) == PORTUGAL) PORTUGUESE_PORTUGAL else PORTUGUESE
            }
            return entries.firstOrNull { it.tag?.substringBefore('-') == language } ?: SYSTEM
        }

        /*
         * Why: a tag's region in upper case ("PT" of pt-PT or pt_pt), null when it has none. It comes before any
         * extension ("-u-fw-mon", Android 14's regional preferences), whose two-letter keys are no region.
         */
        fun regionOf(tag: String?): String? = tag
            ?.split('-', '_')
            ?.drop(1)
            ?.takeWhile { it.length > 1 }
            ?.firstOrNull { it.length == 2 }
            ?.uppercase()

        // Why: a tag's language alone, lower case ("pt" of pt-BR or PT_br), whether the app speaks it or not.
        fun languageOf(tag: String?): String? = tag
            ?.substringBefore('-')
            ?.substringBefore('_')
            ?.lowercase()
            ?.let { if (it == LEGACY_INDONESIAN) INDONESIAN.tag else it }
    }
}
