package com.quare.blitzsplit.core.legal.data.repository

import com.quare.blitzsplit.core.legal.data.datasource.LegalLocalDataSource
import com.quare.blitzsplit.core.legal.data.datasource.LegalRemoteAnswer
import com.quare.blitzsplit.core.legal.data.datasource.LegalRemoteDataSource
import com.quare.blitzsplit.core.legal.data.datasource.SavedLegalText
import com.quare.blitzsplit.core.legal.data.parser.LegalDocumentParser
import com.quare.blitzsplit.core.legal.domain.model.LegalDocument
import com.quare.blitzsplit.core.legal.domain.repository.LegalRepository
import com.quare.blitzsplit.core.model.LegalPage
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class LegalRepositoryImpl(
    private val local: LegalLocalDataSource,
    private val remote: LegalRemoteDataSource,
    private val parser: LegalDocumentParser,
    private val currentTimeMillis: CurrentTimeMillis,
) : LegalRepository {
    /*
     * Why: the launch's download and the screen's may ask for the same text at once; the second waits and finds it
     * fresh, so the site is asked once.
     */
    private val refreshMutex = Mutex()

    // Why: the texts change a few times a year; a day keeps a change from waiting long without asking every launch.
    private val freshFor = 24.hours

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
    ): Result<Unit> = refreshMutex.withLock {
        suspendRunCatching {
            val saved = local.getSavedText(
                page = page,
                language = language,
            )
            if (saved?.isFresh() != true) {
                download(
                    page = page,
                    language = language,
                    savedEtag = saved?.etag,
                )
            }
        }
    }

    /*
     * Why: the site is asked with the saved text's ETag, so an unchanged text answers 304 with no body and only its
     * check time moves.
     */
    private suspend fun download(
        page: LegalPage,
        language: String,
        savedEtag: String?,
    ) {
        val answer = fetchReadable(
            page = page,
            language = language,
            etag = savedEtag,
        ) ?: fetchFallback(
            page = page,
            language = language,
            etag = savedEtag,
        )
        checkNotNull(answer) { "The site has no ${page.slug} to read" }
        val now = currentTimeMillis()
        // Why: a readable answer is a new text or Unchanged; Missing was already left for the fallback.
        if (answer is LegalRemoteAnswer.Text) {
            local.saveMarkdown(
                page = page,
                language = language,
                markdown = answer.markdown,
                etag = answer.etag,
                checkedAt = now,
            )
        } else {
            local.markChecked(
                page = page,
                language = language,
                checkedAt = now,
            )
        }
    }

    /*
     * Why: only a text the app can read replaces the saved one, never an error page a proxy answered with; a language
     * answered with anything else (a host's 200 for a missing file) still tries the English one.
     */
    private suspend fun fetchReadable(
        page: LegalPage,
        language: String,
        etag: String?,
    ): LegalRemoteAnswer? = when (
        val answer = remote.fetchMarkdown(
            page = page,
            language = language,
            etag = etag,
        )
    ) {
        is LegalRemoteAnswer.Text -> answer.takeIf { parser.parse(it.markdown) != null }
        LegalRemoteAnswer.Unchanged -> answer
        LegalRemoteAnswer.Missing -> null
    }

    /*
     * Why: a language the site doesn't write the texts in keeps the English one under its own name, so the saved ETag
     * is English's and an unchanged English text answers 304 too.
     */
    private suspend fun fetchFallback(
        page: LegalPage,
        language: String,
        etag: String?,
    ): LegalRemoteAnswer? = if (language == FALLBACK_LANGUAGE) {
        null
    } else {
        fetchReadable(
            page = page,
            language = FALLBACK_LANGUAGE,
            etag = etag,
        )
    }

    // Why: a clock set back since the last check never keeps a text from being checked again.
    private fun SavedLegalText.isFresh(): Boolean {
        val lastCheck = checkedAt ?: return false
        val elapsed = (currentTimeMillis() - lastCheck).milliseconds
        return elapsed >= Duration.ZERO && elapsed < freshFor
    }

    private companion object {
        // Why: a language the site doesn't write the legal texts in reads them in English, as on the site.
        const val FALLBACK_LANGUAGE = "en"
    }
}
