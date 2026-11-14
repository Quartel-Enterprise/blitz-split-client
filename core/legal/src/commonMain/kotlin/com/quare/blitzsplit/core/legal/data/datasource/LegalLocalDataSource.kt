package com.quare.blitzsplit.core.legal.data.datasource

import com.quare.blitzsplit.core.model.LegalPage
import kotlinx.coroutines.flow.Flow

interface LegalLocalDataSource {
    // Why: the Markdown last saved for the page in that language, null while none was.
    fun observeMarkdown(
        page: LegalPage,
        language: String,
    ): Flow<String?>

    // Why: the ETag and check time of the text saved for the page in that language, null while none was.
    suspend fun getSavedText(
        page: LegalPage,
        language: String,
    ): SavedLegalText?

    suspend fun saveMarkdown(
        page: LegalPage,
        language: String,
        markdown: String,
        etag: String?,
        checkedAt: Long,
    )

    // Why: the site answered that the saved text is still the current one.
    suspend fun markChecked(
        page: LegalPage,
        language: String,
        checkedAt: Long,
    )
}
