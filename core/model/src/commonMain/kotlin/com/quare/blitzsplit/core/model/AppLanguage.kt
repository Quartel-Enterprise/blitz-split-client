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
    SPANISH_SPAIN("es-ES"),
    FRENCH("fr"),
    GERMAN("de"),
    ITALIAN("it"),
    POLISH("pl"),
    DUTCH("nl"),
    JAPANESE("ja"),
    TURKISH("tr"),
    INDONESIAN("id"),
    RUSSIAN("ru"),
    UKRAINIAN("uk"),
    KOREAN("ko"),
    HINDI("hi"),
    ARABIC("ar"),
    ;

    /*
     * Why: what the language picker's chip shows ("EN", "FR"): the language's code, but a language written for more
     * than one region shows each one's region, so its chips differ: "BR" and "PT", "419" (Latin America's UN code,
     * as in es-419, its tag having no region) and "ES". Ukrainian shows Ukraine's "UA", since its code's "UK" reads as
     * the United Kingdom.
     */
    val chipCode: String?
        get() = when {
            this == SPANISH -> LATIN_AMERICA
            this == UKRAINIAN -> UKRAINE
            variantsOf(languageOf(tag)).size > 1 -> regionOf(tag)
            else -> languageOf(tag)?.uppercase()
        }

    companion object {
        // Why: Android still names Indonesian by its legacy code "in" (values-in), so a tag read from it may carry it.
        private const val LEGACY_INDONESIAN = "in"

        private const val LATIN_AMERICA = "419"

        private const val UKRAINE = "UA"

        /*
         * Why: any Portuguese, Spanish, French, German, Italian, Polish, Dutch, Japanese, Turkish, Indonesian, Russian,
         * Ukrainian, Korean, Hindi, Arabic or English tag the platform kept (pt-BR, es-ES, fr-CA, de-AT, it-CH, pl-PL,
         * nl-BE, ja-JP, tr-CY, id-ID, ru-KZ, uk-UA, ko-KR, hi-IN, ar-EG, en-GB), SYSTEM for none or any other. Portuguese is two
         * entries: pt-PT is Portugal's, every other tag (pt, pt-BR…) Brazil's. Spanish too: es-ES is Spain's, every
         * other tag (es, es-MX, es-419…) the neutral Latin-American one.
         */
        fun fromTag(tag: String?): AppLanguage {
            val variants = variantsOf(languageOf(tag))
            val region = regionOf(tag)
            return variants.firstOrNull { region != null && regionOf(it.tag) == region }
                ?: variants.firstOrNull()
                ?: SYSTEM
        }

        // Why: the entries of a language, the one for every other tag first (pt-BR before pt-PT, es before es-ES).
        private fun variantsOf(language: String?): List<AppLanguage> = if (language == null) {
            emptyList()
        } else {
            entries.filter { it.tag != null && languageOf(it.tag) == language }
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
