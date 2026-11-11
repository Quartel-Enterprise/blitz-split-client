package com.quare.blitzsplit.core.legal.domain.usecase.impl

import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.repository.LegalRepository
import com.quare.blitzsplit.core.legal.domain.usecase.ObserveLegalDocument
import com.quare.blitzsplit.core.model.LegalPage
import kotlinx.coroutines.flow.Flow

class ObserveLegalDocumentUseCase(
    private val repository: LegalRepository,
) : ObserveLegalDocument {
    override fun invoke(
        page: LegalPage,
        language: String,
    ): Flow<LegalDocument?> = repository.observeDocument(
        page = page,
        language = language,
    )
}
