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

    // Why: Spain's Spanish (es-ES) only overrides the words Spain says differently, so a device in Spain keeps them.
    fun toTag(region: String?): String? = if (this == SPANISH &&
        region.equals("ES", ignoreCase = true)
    ) {
        "es-ES"
    } else {
        tag
    }

    companion object {
        // Why: Java's Locale named Indonesian by its legacy code, which an older stored tag may still carry.
        private const val LEGACY_INDONESIAN = "in"

        /*
         * Why: any Portuguese, Spanish, French, German, Italian, Polish, Dutch, Japanese, Turkish, Indonesian or English
         * tag the platform kept (pt-PT, es-ES, fr-CA, de-AT, it-CH, pl-PL, nl-BE, ja-JP, tr-CY, id-ID, en-GB), SYSTEM for
         * none or any other.
         */
        fun fromTag(tag: String?): AppLanguage {
            val language = tag
                ?.substringBefore('-')
                ?.substringBefore('_')
                ?.lowercase()
                ?.let { if (it == LEGACY_INDONESIAN) INDONESIAN.tag else it }
            return entries.firstOrNull { it.tag?.substringBefore('-') == language } ?: SYSTEM
        }
    }
}
