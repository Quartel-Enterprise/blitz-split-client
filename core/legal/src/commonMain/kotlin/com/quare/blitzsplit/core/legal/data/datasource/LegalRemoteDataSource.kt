package com.quare.blitzsplit.core.legal.data.datasource

import com.quare.blitzsplit.core.model.LegalPage

fun interface LegalRemoteDataSource {
    // Why: the page's Markdown in that language, null when the site doesn't have the page in it. Throws offline.
    suspend fun fetchMarkdown(
        page: LegalPage,
        language: String,
    ): String?
}
