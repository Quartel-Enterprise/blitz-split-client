package com.quare.blitzsplit.core.legal.domain.usecase

import com.quare.blitzsplit.core.model.LegalPage

// Why: downloads the page's current text in the reader's language; fails offline.
fun interface RefreshLegalDocument {
    suspend operator fun invoke(page: LegalPage): Result<Unit>
}
