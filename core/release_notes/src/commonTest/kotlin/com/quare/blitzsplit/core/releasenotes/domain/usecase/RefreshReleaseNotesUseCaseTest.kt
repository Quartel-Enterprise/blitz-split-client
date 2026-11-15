package com.quare.blitzsplit.core.releasenotes.domain.usecase

import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.RefreshReleaseNotesUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefreshReleaseNotesUseCaseTest {
    private lateinit var repository: FakeReleaseNotesRepository
    private lateinit var refresh: RefreshReleaseNotesUseCase

    @Test
    fun `GIVEN the site answers WHEN refreshing in Portuguese THEN refreshes that feed and succeeds`() = runTest {
        // Given
        prepareScenario()

        // When
        val result = refresh("pt")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("pt"), repository.refreshed)
        assertEquals(listOf(AppVersion.parseOrNull("3.1.0-beta_2")), repository.refreshedVersions)
    }

    @Test
    fun `GIVEN no internet WHEN refreshing THEN hands the failure back`() = runTest {
        // Given
        val offline = IllegalStateException("offline")
        prepareScenario(refreshResult = Result.failure(offline))

        // When
        val result = refresh("pt")

        // Then
        assertEquals(Result.failure(offline), result)
    }

    private fun prepareScenario(refreshResult: Result<Unit> = Result.success(Unit)) {
        repository = FakeReleaseNotesRepository().also { it.refreshResult = refreshResult }
        refresh = RefreshReleaseNotesUseCase(
            repository = repository,
            getInstalledVersion = { AppVersion.parseOrNull("3.1.0-beta_2") },
        )
    }
}
