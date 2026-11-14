package com.quare.blitzsplit.core.model

/*
 * Why: a link the app shares says which language the app speaks, "?lang=pt", for the links whose preview (WhatsApp,
 * iMessage) has nothing else to pick a language by: a profile's, a comment's, "Get the app". blitzsplit.com writes
 * the preview in it, and its pages read the same parameter, so the page that opens speaks it too.
 */
object LinkLanguage {
    private const val PARAMETER = "lang"

    fun buildLink(
        link: String,
        language: String,
    ): String = link + (if ('?' in link) '&' else '?') + PARAMETER + '=' + language
}
