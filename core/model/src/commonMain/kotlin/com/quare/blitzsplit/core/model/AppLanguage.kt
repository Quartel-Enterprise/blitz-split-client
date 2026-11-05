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
    ;

    companion object {
        // Why: any Portuguese (pt, pt-BR, pt-PT) or English tag the platform kept, SYSTEM for none or any other language.
        fun fromTag(tag: String?): AppLanguage {
            val language = tag
                ?.substringBefore('-')
                ?.substringBefore('_')
                ?.lowercase()
            return entries.firstOrNull { it.tag?.substringBefore('-') == language } ?: SYSTEM
        }
    }
}
