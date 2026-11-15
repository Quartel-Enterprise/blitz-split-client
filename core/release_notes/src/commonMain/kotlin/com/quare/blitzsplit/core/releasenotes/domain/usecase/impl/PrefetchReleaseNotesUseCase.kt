package com.quare.blitzsplit.core.releasenotes.domain.usecase.impl

import com.quare.blitzsplit.core.releasenotes.domain.repository.ReleaseNotesRepository
import com.quare.blitzsplit.core.releasenotes.domain.usecase.GetReleaseNotesLanguage
import com.quare.blitzsplit.core.releasenotes.domain.usecase.PrefetchReleaseNotes

class PrefetchReleaseNotesUseCase(
    private val getReleaseNotesLanguage: GetReleaseNotesLanguage,
    private val repository: ReleaseNotesRepository,
) : PrefetchReleaseNotes {
    override suspend fun invoke() {
        repository.refreshReleases(getReleaseNotesLanguage())
    }
}
