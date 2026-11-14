package com.quare.blitzsplit.core.legal.data.datasource

sealed interface LegalRemoteAnswer {
    // Why: the page's Markdown, with the site's ETag for it (null when it sent none).
    data class Text(
        val markdown: String,
        val etag: String?,
    ) : LegalRemoteAnswer

    // Why: 304, the text whose ETag the app sent is still the current one.
    data object Unchanged : LegalRemoteAnswer

    // Why: 404, the site doesn't write the page in that language.
    data object Missing : LegalRemoteAnswer
}
