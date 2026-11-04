package com.quare.blitzsplit.core.inappupdate.domain.usecase

import com.quare.blitzsplit.core.inappupdate.domain.usecase.impl.InstallDownloadedUpdateUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InstallDownloadedUpdateUseCaseTest {
    private lateinit var useCase: InstallDownloadedUpdateUseCase
    private var installs = 0
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()

    @Test
    fun `GIVEN a downloaded update WHEN installing it THEN tracks the install and installs it`() = runTest {
        // Given
        prepareScenario()

        // When
        useCase()

        // Then
        assertEquals(listOf("update_install_started" to emptyMap<String, Any>()), trackedEvents)
        assertEquals(1, installs)
    }

    @Test
    fun `GIVEN an install that did not go through WHEN installing again THEN installs without tracking it again`() =
        runTest {
            // Given
            prepareScenario()
            useCase()

            // When
            useCase()

            // Then
            assertEquals(2, installs)
            assertEquals(listOf("update_install_started" to emptyMap<String, Any>()), trackedEvents)
        }

    private fun prepareScenario() {
        useCase = InstallDownloadedUpdateUseCase(
            completeUpdateInstall = { installs++ },
            trackEvent = { name, params -> trackedEvents += name to params },
        )
    }
}
