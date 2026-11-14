package com.quare.blitzsplit.core.legal.data.datasource

import com.quare.blitzsplit.core.model.LegalPage

fun interface LegalRemoteDataSource {
    /*
     * Why: the page's Markdown in that language. With the ETag of the text the device holds, an unchanged text answers
     * Unchanged without its body. Throws offline.
     */
    suspend fun fetchMarkdown(
        page: LegalPage,
        language: String,
        etag: String?,
    ): LegalRemoteAnswer
}
