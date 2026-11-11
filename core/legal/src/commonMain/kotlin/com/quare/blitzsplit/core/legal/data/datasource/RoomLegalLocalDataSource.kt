package com.quare.blitzsplit.core.legal.data.datasource

import com.quare.blitzsplit.core.model.LegalPage
import com.quare.blitzsplit.core.provider.room.dao.LegalDocumentDao
import com.quare.blitzsplit.core.provider.room.entity.LegalDocumentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomLegalLocalDataSource(
    private val dao: LegalDocumentDao,
) : LegalLocalDataSource {
    override fun observeMarkdown(
        page: LegalPage,
        language: String,
    ): Flow<String?> = dao
        .observe(
            page = page.slug,
            language = language,
        ).map { it?.markdown }

    override suspend fun saveMarkdown(
        page: LegalPage,
        language: String,
        markdown: String,
    ) {
        dao.upsert(
            LegalDocumentEntity(
                page = page.slug,
                language = language,
                markdown = markdown,
            ),
        )
    }
}
