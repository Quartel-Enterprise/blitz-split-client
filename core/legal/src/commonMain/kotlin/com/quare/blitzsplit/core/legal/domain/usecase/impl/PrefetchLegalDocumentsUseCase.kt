package com.quare.blitzsplit.core.legal.domain.usecase.impl

import com.quare.blitzsplit.core.legal.domain.repository.LegalRepository
import com.quare.blitzsplit.core.legal.domain.usecase.GetLegalLanguage
import com.quare.blitzsplit.core.legal.domain.usecase.PrefetchLegalDocuments
import com.quare.blitzsplit.core.model.LegalPage

class PrefetchLegalDocumentsUseCase(
    // Why: false on the web, which opens the site's own pages and never draws the texts itself.
    private val drawsTextsInApp: Boolean,
    private val getLegalLanguage: GetLegalLanguage,
    private val repository: LegalRepository,
) : PrefetchLegalDocuments {
    override suspend fun invoke() {
        if (!drawsTextsInApp) return
        val language = getLegalLanguage()
        // Why: one that fails offline doesn't keep the other from trying.
        LegalPage.entries.forEach { page ->
            repository.refreshDocument(
                page = page,
                language = language,
            )
        }
    }
}
