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

    override suspend fun getSavedText(
        page: LegalPage,
        language: String,
    ): SavedLegalText? = dao
        .find(
            page = page.slug,
            language = language,
        )?.let { entity ->
            SavedLegalText(
                etag = entity.etag,
                checkedAt = entity.checkedAt,
            )
        }

    override suspend fun saveMarkdown(
        page: LegalPage,
        language: String,
        markdown: String,
        etag: String?,
        checkedAt: Long,
    ) {
        dao.upsert(
            LegalDocumentEntity(
                page = page.slug,
                language = language,
                markdown = markdown,
                etag = etag,
                checkedAt = checkedAt,
            ),
        )
    }

    override suspend fun markChecked(
        page: LegalPage,
        language: String,
        checkedAt: Long,
    ) {
        dao.markChecked(
            page = page.slug,
            language = language,
            checkedAt = checkedAt,
        )
    }
}
