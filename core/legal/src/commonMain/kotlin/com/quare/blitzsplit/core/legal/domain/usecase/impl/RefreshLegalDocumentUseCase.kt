package com.quare.blitzsplit.core.legal.domain.usecase.impl

import com.quare.blitzsplit.core.legal.domain.repository.LegalRepository
import com.quare.blitzsplit.core.legal.domain.usecase.RefreshLegalDocument
import com.quare.blitzsplit.core.model.LegalPage

class RefreshLegalDocumentUseCase(
    private val repository: LegalRepository,
) : RefreshLegalDocument {
    override suspend fun invoke(
        page: LegalPage,
        language: String,
    ): Result<Unit> = repository.refreshDocument(
        page = page,
        language = language,
    )
}
