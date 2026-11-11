package com.quare.blitzsplit.core.legal.domain.usecase.impl

import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.repository.LegalRepository
import com.quare.blitzsplit.core.legal.domain.usecase.GetLegalLanguage
import com.quare.blitzsplit.core.legal.domain.usecase.ObserveLegalDocument
import com.quare.blitzsplit.core.model.LegalPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ObserveLegalDocumentUseCase(
    private val repository: LegalRepository,
    private val getLegalLanguage: GetLegalLanguage,
) : ObserveLegalDocument {
    override fun invoke(page: LegalPage): Flow<LegalDocument?> = flow {
        emitAll(
            repository.observeDocument(
                page = page,
                language = getLegalLanguage(),
            ),
        )
    }
}
