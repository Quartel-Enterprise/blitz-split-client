package com.quare.blitzsplit.core.releasenotes.domain.usecase

import com.quare.blitzsplit.core.releasenotes.domain.model.PublishedRelease
import com.quare.blitzsplit.core.releasenotes.domain.repository.ReleaseNotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// Why: the use case tests of this module share it; no other module reads the release notes' repository.
internal class FakeReleaseNotesRepository : ReleaseNotesRepository {
    val releases = MutableStateFlow<Map<String, List<PublishedRelease>>>(emptyMap())
    val observed = mutableListOf<String>()
    val refreshed = mutableListOf<String>()
    var refreshResult: Result<Unit> = Result.success(Unit)

    override fun observeReleases(language: String): Flow<List<PublishedRelease>?> {
        observed += language
        return releases.map { it[language] }
    }

    override suspend fun refreshReleases(language: String): Result<Unit> {
        refreshed += language
        return refreshResult
    }
}
