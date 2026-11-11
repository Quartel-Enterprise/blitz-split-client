package com.quare.blitzsplit.core.legal.domain.usecase.impl

import com.quare.blitzsplit.core.legal.domain.repository.LegalRepository
import com.quare.blitzsplit.core.legal.domain.usecase.GetLegalLanguage
import com.quare.blitzsplit.core.legal.domain.usecase.RefreshLegalDocument
import com.quare.blitzsplit.core.model.LegalPage

class RefreshLegalDocumentUseCase(
    private val repository: LegalRepository,
    private val getLegalLanguage: GetLegalLanguage,
) : RefreshLegalDocument {
    override suspend fun invoke(page: LegalPage): Result<Unit> = repository.refreshDocument(
        page = page,
        language = getLegalLanguage(),
    )
}
