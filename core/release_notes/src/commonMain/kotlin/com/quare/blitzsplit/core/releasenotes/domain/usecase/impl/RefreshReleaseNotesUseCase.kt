package com.quare.blitzsplit.core.releasenotes.domain.usecase.impl

import com.quare.blitzsplit.core.releasenotes.domain.repository.ReleaseNotesRepository
import com.quare.blitzsplit.core.releasenotes.domain.usecase.RefreshReleaseNotes

class RefreshReleaseNotesUseCase(
    private val repository: ReleaseNotesRepository,
) : RefreshReleaseNotes {
    override suspend fun invoke(language: String): Result<Unit> = repository.refreshReleases(language)
}
