package com.quare.blitzsplit.core.releasenotes.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.ReleaseNotesFeedDao
import com.quare.blitzsplit.core.provider.room.entity.ReleaseNotesFeedEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomReleaseNotesLocalDataSource(
    private val dao: ReleaseNotesFeedDao,
) : ReleaseNotesLocalDataSource {
    override fun observeFeed(language: String): Flow<String?> = dao.observe(language).map { it?.json }

    override suspend fun getSavedFeed(language: String): SavedReleaseNotesFeed? = dao.find(language)?.let { entity ->
        SavedReleaseNotesFeed(
            etag = entity.etag,
            checkedAt = entity.checkedAt,
        )
    }

    override suspend fun saveFeed(
        language: String,
        json: String,
        etag: String?,
        checkedAt: Long,
    ) {
        dao.upsert(
            ReleaseNotesFeedEntity(
                language = language,
                json = json,
                etag = etag,
                checkedAt = checkedAt,
            ),
        )
    }

    override suspend fun markChecked(
        language: String,
        checkedAt: Long,
    ) {
        dao.markChecked(
            language = language,
            checkedAt = checkedAt,
        )
    }
}
