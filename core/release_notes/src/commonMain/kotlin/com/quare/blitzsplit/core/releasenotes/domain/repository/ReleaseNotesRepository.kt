package com.quare.blitzsplit.core.releasenotes.domain.repository

import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease
import kotlinx.coroutines.flow.Flow

interface ReleaseNotesRepository {
    // Why: the versions the device last saved for that language, newest first; null until it downloaded them.
    fun observeReleases(language: String): Flow<List<PublishedRelease>?>

    /*
     * Why: asks the site only when a day passed since it last confirmed the saved ones, or 15 minutes in the first
     * hours of an installed version they don't list yet; null for a version whose name can't be read.
     */
    suspend fun refreshReleases(
        language: String,
        installedVersion: AppVersion?,
    ): Result<Unit>
}
