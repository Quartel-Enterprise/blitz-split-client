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
    CHINESE_SIMPLIFIED("zh-Hans"),
    CHINESE_TRADITIONAL("zh-Hant"),
    HEBREW("he"),
    THAI("th"),
    VIETNAMESE("vi"),
    SWEDISH("sv"),
    CZECH("cs"),
    ROMANIAN("ro"),
    BENGALI("bn"),
    MALAY("ms"),
    GREEK("el"),
    CROATIAN("hr"),
    HUNGARIAN("hu"),
    URDU("ur"),
    FILIPINO("fil"),
    SERBIAN("sr-Latn"),
    PERSIAN("fa"),
    ;

    /*
     * Why: what the language picker's chip shows ("EN", "FR"): the language's code, but a language written for more
     * than one region shows each one's region, so its chips differ: "BR" and "PT", "419" (Latin America's UN code,
     * as in es-419, its tag having no region) and "ES". Ukrainian shows Ukraine's "UA", since its code's "UK" reads as
     * the United Kingdom. Chinese, whose two entries differ by script, shows the region each is written for, "CN" for
     * Simplified and "TW" for Traditional.
     */
    val chipCode: String?
        get() = when {
            this == SPANISH -> LATIN_AMERICA
            this == UKRAINIAN -> UKRAINE
            this == CHINESE_SIMPLIFIED -> MAINLAND_CHINA
            this == CHINESE_TRADITIONAL -> TAIWAN
            variantsOf(languageOf(tag)).size > 1 -> regionOf(tag)
            else -> languageOf(tag)?.uppercase()
        }

    /*
     * Why: the code the app asks blitzsplit.com and its link previews by (release notes, legal pages, "?lang="): the
     * language's own ("pt", "es", "zh"), but Traditional Chinese's whole tag, "zh-Hant", since the bare "zh" is
     * Simplified's, as "pt" is Brazil's; null for SYSTEM.
     */
    val spokenCode: String?
        get() = if (this == CHINESE_TRADITIONAL) tag else languageOf(tag)

    companion object {
        // Why: Android still names Indonesian by its legacy code "in" (values-in), so a tag read from it may carry it.
        private const val LEGACY_INDONESIAN = "in"

        // Why: Android also names Hebrew by its legacy code "iw" (values-iw), and Java's Locale still hands it out.
        private const val LEGACY_HEBREW = "iw"

        // Why: Java and older Android name Filipino by Tagalog's code "tl", which a tag read from them may carry.
        private const val LEGACY_TAGALOG = "tl"

        private const val LATIN_AMERICA = "419"

        private const val UKRAINE = "UA"

        private const val CHINESE = "zh"

        /*
         * Why: the app has Serbian in the Latin script only, so a Cyrillic tag, or one with no script (Cyrillic by
         * default), finds no strings of its own: it reads as a language the app doesn't have, English, as the devices show it.
         */
        private const val SERBIAN_LANGUAGE = "sr"

        private const val LATIN_SCRIPT = "LATN"

        private const val MAINLAND_CHINA = "CN"

        private const val TRADITIONAL_SCRIPT = "HANT"

        private const val TAIWAN = "TW"

        private const val HONG_KONG = "HK"

        private const val MACAU = "MO"

        /*
         * Why: any Portuguese, Spanish, French, German, Italian, Polish, Dutch, Japanese, Turkish, Indonesian, Russian,
         * Ukrainian, Korean, Hindi, Arabic, Chinese, Hebrew, Thai, Vietnamese, Swedish, Czech, Romanian, Bengali, Malay,
         * Greek, Croatian, Hungarian, Urdu, Filipino (or its legacy Tagalog "tl"), Serbian in the Latin script (sr-Latn, sr-Latn-RS), Persian or English tag the platform kept (pt-BR, es-ES, fr-CA, de-AT, it-CH, pl-PL, nl-BE,
         * ja-JP, tr-CY, id-ID, ru-KZ, uk-UA, ko-KR, hi-IN, ar-EG, zh-CN, he-IL or the legacy iw-IL, th-TH, vi-VN, sv-FI,
         * cs-CZ, ro-MD, bn-IN, ms-SG, el-CY, hr-BA, hu-HU, ur-PK, fil-PH, sr-Latn-RS, fa-IR or fa-AF, en-GB), SYSTEM for none or any other. Portuguese is two
         * entries: pt-PT is Portugal's, every other tag (pt, pt-BR…) Brazil's. Spanish too: es-ES is Spain's, every
         * other tag (es, es-MX, es-419…) the neutral Latin-American one. Chinese is two entries too: a Traditional tag
         * (zh-Hant, zh-TW, zh-HK, zh-MO) is Taiwan's Traditional Chinese, zh-Hant, every other one (zh, zh-CN, zh-Hans,
         * zh-SG…) mainland China's Simplified, zh-Hans.
         */
        fun fromTag(tag: String?): AppLanguage {
            if (isTraditionalChinese(tag)) return CHINESE_TRADITIONAL
            if (languageOf(tag) == SERBIAN_LANGUAGE && scriptOf(tag) != LATIN_SCRIPT) return SYSTEM
            val variants = variantsOf(languageOf(tag))
            val region = regionOf(tag)
            return variants.firstOrNull { region != null && regionOf(it.tag) == region }
                ?: variants.firstOrNull()
                ?: SYSTEM
        }

        /*
         * Why: a Chinese tag written in Traditional characters: its script says so when it has one (zh-Hant-CN is
         * Traditional, zh-Hans-HK Simplified); without one, a region that writes them does (Taiwan, Hong Kong, Macau).
         */
        private fun isTraditionalChinese(tag: String?): Boolean {
            if (languageOf(tag) != CHINESE) return false
            val script = scriptOf(tag) ?: return regionOf(tag).let { it == TAIWAN || it == HONG_KONG || it == MACAU }
            return script == TRADITIONAL_SCRIPT
        }

        // Why: a tag's script in upper case ("HANS" of zh-Hans-CN), before any extension, as regionOf reads its region.
        private fun scriptOf(tag: String?): String? = tag
            ?.split('-', '_')
            ?.drop(1)
            ?.takeWhile { it.length > 1 }
            ?.firstOrNull { it.length == 4 && it.all(Char::isLetter) }
            ?.uppercase()

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
            ?.let {
                when (it) {
                    LEGACY_INDONESIAN -> INDONESIAN.tag
                    LEGACY_HEBREW -> HEBREW.tag
                    LEGACY_TAGALOG -> FILIPINO.tag
                    else -> it
                }
            }
    }
}
