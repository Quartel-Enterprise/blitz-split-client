package com.quare.blitzsplit.core.releasenotes.data.datasource

import kotlinx.coroutines.flow.Flow

interface ReleaseNotesLocalDataSource {
    // Why: the feed's JSON last saved for that language, null while none was.
    fun observeFeed(language: String): Flow<String?>

    // Why: the ETag and check time of the feed saved for that language, null while none was.
    suspend fun getSavedFeed(language: String): SavedReleaseNotesFeed?

    suspend fun saveFeed(
        language: String,
        json: String,
        etag: String?,
        checkedAt: Long,
    )

    // Why: the site answered that the saved feed is still the current one.
    suspend fun markChecked(
        language: String,
        checkedAt: Long,
    )
}
