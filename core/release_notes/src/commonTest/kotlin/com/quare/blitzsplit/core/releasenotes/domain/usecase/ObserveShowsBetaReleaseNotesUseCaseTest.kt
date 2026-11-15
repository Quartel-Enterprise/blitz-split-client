package com.quare.blitzsplit.core.releasenotes.domain.usecase

import com.quare.blitzsplit.core.releasenotes.domain.model.AppVersion
import com.quare.blitzsplit.core.releasenotes.domain.usecase.impl.ObserveShowsBetaReleaseNotesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ObserveShowsBetaReleaseNotesUseCaseTest {
    private lateinit var choice: MutableStateFlow<Boolean?>
    private lateinit var observeShowsBeta: ObserveShowsBetaReleaseNotesUseCase

    @Test
    fun `GIVEN a beta build and nothing switched yet WHEN observing THEN shows the beta versions`() = runTest {
        // Given
        prepareScenario(
            buildVersion = "3.1.0-beta_2",
            switched = null,
        )

        // When
        val shows = observeShowsBeta().first()

        // Then
        assertTrue(shows)
    }

    @Test
    fun `GIVEN a stable build and nothing switched yet WHEN observing THEN hides the beta versions`() = runTest {
        // Given
        prepareScenario(
            buildVersion = "3.1.0",
            switched = null,
        )

        // When
        val shows = observeShowsBeta().first()

        // Then
        assertFalse(shows)
    }

    @Test
    fun `GIVEN a build of unreadable version and nothing switched yet WHEN observing THEN hides the beta versions`() =
        runTest {
            // Given
            prepareScenario(
                buildVersion = "local",
                switched = null,
            )

            // When
            val shows = observeShowsBeta().first()

            // Then
            assertFalse(shows)
        }

    @Test
    fun `GIVEN a stable build where the person switched it on WHEN observing THEN shows the beta versions`() = runTest {
        // Given
        prepareScenario(
            buildVersion = "3.1.0",
            switched = true,
        )

        // When
        val shows = observeShowsBeta().first()

        // Then
        assertTrue(shows)
    }

    @Test
    fun `GIVEN a beta build where the person switched it off WHEN observing THEN hides the beta versions`() = runTest {
        // Given
        prepareScenario(
            buildVersion = "3.1.0-beta_2",
            switched = false,
        )

        // When
        val shows = observeShowsBeta().first()

        // Then
        assertFalse(shows)
    }

    @Test
    fun `GIVEN a stable build observed with nothing switched WHEN the person switches it on and off THEN it follows`() =
        runTest {
            // Given
            prepareScenario(
                buildVersion = "3.1.0",
                switched = null,
            )
            val seen = mutableListOf<Boolean>()
            backgroundScope.launch { observeShowsBeta().collect(seen::add) }
            runCurrent()

            // When
            choice.value = true
            runCurrent()
            choice.value = false
            runCurrent()

            // Then
            assertEquals(listOf(false, true, false), seen)
        }

    private fun prepareScenario(
        buildVersion: String,
        switched: Boolean?,
    ) {
        choice = MutableStateFlow(switched)
        observeShowsBeta = ObserveShowsBetaReleaseNotesUseCase(
            observeBetaReleaseNotesChoice = { choice },
            getInstalledVersion = { AppVersion.parseOrNull(buildVersion) },
        )
    }
}
