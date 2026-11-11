package com.quare.blitzsplit.core.legal.domain.usecase

import com.quare.blitzsplit.core.model.LegalPage

// Why: downloads the page's current text in that language (GetLegalLanguage's); fails offline.
fun interface RefreshLegalDocument {
    suspend operator fun invoke(
        page: LegalPage,
        language: String,
    ): Result<Unit>
}
