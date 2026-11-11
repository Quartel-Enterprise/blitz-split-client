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

    // Why: downloads the page's current text for that language, English's when the site has none in it.
    suspend fun refreshDocument(
        page: LegalPage,
        language: String,
    ): Result<Unit>
}
