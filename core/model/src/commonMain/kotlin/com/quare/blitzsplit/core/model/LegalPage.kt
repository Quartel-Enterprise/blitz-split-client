package com.quare.blitzsplit.core.model

/*
 * Why: the two legal texts Profile's footer opens. slug is the page's name on the site, in its address
 * (blitzsplit.com/privacy) and in its Markdown's (blitzsplit.com/legal/privacy.en.md).
 */
enum class LegalPage(
    val slug: String,
) {
    PRIVACY_POLICY("privacy"),
    TERMS_OF_USE("terms"),
    ;

    companion object {
        fun fromSlug(slug: String): LegalPage? = entries.firstOrNull { it.slug == slug }
    }
}
