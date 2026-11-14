package com.quare.blitzsplit.core.legal.domain.repository

import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.model.LegalPage
import kotlinx.coroutines.flow.Flow

interface LegalRepository {
    // Why: the page as the device last saved it for that language, null while it never downloaded it.
    fun observeDocument(
        page: LegalPage,
        language: String,
    ): Flow<LegalDocument?>

    /*
     * Why: makes sure the saved text for that language is the page's current one (English's when the site has none in
     * it), asking the site at most once a day and downloading the text only when it changed.
     */
    suspend fun refreshDocument(
        page: LegalPage,
        language: String,
    ): Result<Unit>
}
