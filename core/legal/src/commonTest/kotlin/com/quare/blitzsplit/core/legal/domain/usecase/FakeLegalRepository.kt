package com.quare.blitzsplit.core.legal.domain.usecase

import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.repository.LegalRepository
import com.quare.blitzsplit.core.model.LegalPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// Why: the use case tests of this module share it; no other module reads the legal texts.
internal class FakeLegalRepository : LegalRepository {
    val documents = MutableStateFlow<Map<Pair<LegalPage, String>, LegalDocument>>(emptyMap())
    val observed = mutableListOf<Pair<LegalPage, String>>()
    val refreshed = mutableListOf<Pair<LegalPage, String>>()
    var refreshResult: Result<Unit> = Result.success(Unit)

    override fun observeDocument(
        page: LegalPage,
        language: String,
    ): Flow<LegalDocument?> {
        observed += page to language
        return documents.map { it[page to language] }
    }

    override suspend fun refreshDocument(
        page: LegalPage,
        language: String,
    ): Result<Unit> {
        refreshed += page to language
        return refreshResult
    }
}
