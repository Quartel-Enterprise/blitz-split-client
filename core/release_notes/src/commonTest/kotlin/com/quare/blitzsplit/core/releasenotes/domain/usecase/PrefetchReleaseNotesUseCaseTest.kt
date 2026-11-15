package com.quare.blitzsplit.core.releasenotes.domain.usecase

import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.PrefetchReleaseNotesUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PrefetchReleaseNotesUseCaseTest {
    private lateinit var repository: FakeReleaseNotesRepository
    private lateinit var prefetch: PrefetchReleaseNotesUseCase

    @Test
    fun `GIVEN an app speaking Turkish WHEN prefetching THEN refreshes the Turkish feed`() = runTest {
        // Given
        prepareScenario(language = "tr")

        // When
        prefetch()

        // Then
        assertEquals(listOf("tr"), repository.refreshed)
        assertEquals(listOf(AppVersion.parseOrNull("3.1.0")), repository.refreshedVersions)
    }

    @Test
    fun `GIVEN no internet WHEN prefetching THEN tries the download and does not throw`() = runTest {
        // Given
        prepareScenario(
            language = "pt",
            refreshResult = Result.failure(IllegalStateException("offline")),
        )

        // When
        val outcome = prefetch()

        // Then
        assertEquals(Unit, outcome)
        assertEquals(listOf("pt"), repository.refreshed)
    }

    private fun prepareScenario(
        language: String,
        refreshResult: Result<Unit> = Result.success(Unit),
    ) {
        repository = FakeReleaseNotesRepository().also { it.refreshResult = refreshResult }
        prefetch = PrefetchReleaseNotesUseCase(
            getReleaseNotesLanguage = { language },
            repository = repository,
            getInstalledVersion = { AppVersion.parseOrNull("3.1.0") },
        )
    }
}
