package com.quare.blitzsplit.core.releasenotes.domain.usecase.impl

import com.quare.blitzsplit.core.releasenotes.domain.repository.ReleaseNotesRepository
import com.quare.blitzsplit.core.releasenotes.domain.usecase.GetInstalledVersion
import com.quare.blitzsplit.core.releasenotes.domain.usecase.GetReleaseNotesLanguage
import com.quare.blitzsplit.core.releasenotes.domain.usecase.PrefetchReleaseNotes

class PrefetchReleaseNotesUseCase(
    private val getReleaseNotesLanguage: GetReleaseNotesLanguage,
    private val repository: ReleaseNotesRepository,
    private val getInstalledVersion: GetInstalledVersion,
) : PrefetchReleaseNotes {
    override suspend fun invoke() {
        repository.refreshReleases(
            language = getReleaseNotesLanguage(),
            installedVersion = getInstalledVersion(),
        )
    }
}
