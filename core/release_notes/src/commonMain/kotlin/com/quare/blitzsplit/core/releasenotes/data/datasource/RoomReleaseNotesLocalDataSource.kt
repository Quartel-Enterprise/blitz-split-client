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
            json = entity.json,
            etag = entity.etag,
            checkedAt = entity.checkedAt,
            installedVersion = entity.installedVersion?.let { name ->
                entity.installedVersionSeenAt?.let { seenAt ->
                    SeenInstalledVersion(
                        name = name,
                        seenAt = seenAt,
                    )
                }
            },
        )
    }

    override suspend fun saveFeed(
        language: String,
        json: String,
        etag: String?,
        checkedAt: Long,
        installedVersion: SeenInstalledVersion?,
    ) {
        dao.upsert(
            ReleaseNotesFeedEntity(
                language = language,
                json = json,
                etag = etag,
                checkedAt = checkedAt,
                installedVersion = installedVersion?.name,
                installedVersionSeenAt = installedVersion?.seenAt,
            ),
        )
    }

    override suspend fun markChecked(
        language: String,
        checkedAt: Long,
        installedVersion: SeenInstalledVersion?,
    ) {
        dao.markChecked(
            language = language,
            checkedAt = checkedAt,
            installedVersion = installedVersion?.name,
            installedVersionSeenAt = installedVersion?.seenAt,
        )
    }
}
