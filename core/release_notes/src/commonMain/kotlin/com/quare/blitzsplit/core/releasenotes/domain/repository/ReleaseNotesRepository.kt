package com.quare.blitzsplit.core.releasenotes.domain.repository

import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease
import kotlinx.coroutines.flow.Flow

interface ReleaseNotesRepository {
    // Why: the versions the device last saved for that language, newest first; null until it downloaded them.
    fun observeReleases(language: String): Flow<List<PublishedRelease>?>

    // Why: asks the site only when a day passed since it last confirmed the saved ones.
    suspend fun refreshReleases(language: String): Result<Unit>
}
