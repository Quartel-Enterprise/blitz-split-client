package com.quare.blitzsplit.core.legal.domain.usecase

import com.quare.blitzsplit.core.model.LegalPage

/*
 * Why: brings the page's text in that language (GetLegalLanguage's) up to date, asking the site at most once a day;
 * fails offline when the saved text is older than that, or when there is none.
 */
fun interface RefreshLegalDocument {
    suspend operator fun invoke(
        page: LegalPage,
        language: String,
    ): Result<Unit>
}
