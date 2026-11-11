package com.quare.blitzsplit.core.legal.data.datasource

import com.quare.blitzsplit.core.model.LegalPage
import kotlinx.coroutines.flow.Flow

interface LegalLocalDataSource {
    // Why: the Markdown last saved for the page in that language, null while none was.
    fun observeMarkdown(
        page: LegalPage,
        language: String,
    ): Flow<String?>

    suspend fun saveMarkdown(
        page: LegalPage,
        language: String,
        markdown: String,
    )
}
