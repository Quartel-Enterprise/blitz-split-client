package com.quare.blitzsplit.core.legal.domain.usecase

import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.model.LegalPage
import kotlinx.coroutines.flow.Flow

// Why: the page in that language (GetLegalLanguage's) as the device holds it, null while it never downloaded it.
fun interface ObserveLegalDocument {
    operator fun invoke(
        page: LegalPage,
        language: String,
    ): Flow<LegalDocument?>
}
