package com.quare.blitzsplit.core.legal.data.repository

import com.quare.blitzsplit.core.legal.data.datasource.LegalLocalDataSource
import com.quare.blitzsplit.core.legal.data.datasource.LegalRemoteDataSource
import com.quare.blitzsplit.core.legal.data.parser.LegalDocumentParser
import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.repository.LegalRepository
import com.quare.blitzsplit.core.model.LegalPage
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class LegalRepositoryImpl(
    private val local: LegalLocalDataSource,
    private val remote: LegalRemoteDataSource,
    private val parser: LegalDocumentParser,
) : LegalRepository {
    override fun observeDocument(
        page: LegalPage,
        language: String,
    ): Flow<LegalDocument?> = local
        .observeMarkdown(
            page = page,
            language = language,
        ).distinctUntilChanged()
        .map { markdown -> markdown?.let(parser::parse) }

    override suspend fun refreshDocument(
        page: LegalPage,
        language: String,
    ): Result<Unit> = suspendRunCatching {
        val markdown = remote.fetchMarkdown(
            page = page,
            language = language,
        ) ?: fetchFallback(
            page = page,
            language = language,
        )
        // Why: only a text the app can read replaces the saved one, never an error page a proxy answered with.
        checkNotNull(markdown?.takeIf { parser.parse(it) != null }) { "The site has no ${page.slug} to read" }
        local.saveMarkdown(
            page = page,
            language = language,
            markdown = markdown,
        )
    }

    private suspend fun fetchFallback(
        page: LegalPage,
        language: String,
    ): String? = if (language == FALLBACK_LANGUAGE) {
        null
    } else {
        remote.fetchMarkdown(
            page = page,
            language = FALLBACK_LANGUAGE,
        )
    }

    private companion object {
        // Why: French, German, Italian and Polish read the legal texts in English, as on the site.
        const val FALLBACK_LANGUAGE = "en"
    }
}
