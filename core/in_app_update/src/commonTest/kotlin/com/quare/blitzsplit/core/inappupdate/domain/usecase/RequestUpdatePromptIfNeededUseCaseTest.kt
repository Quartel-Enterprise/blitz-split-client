package com.quare.blitzsplit.core.inappupdate.domain.usecase

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateMechanism
import com.quare.blitzsplit.core.inappupdate.domain.usecase.impl.RequestUpdatePromptIfNeededUseCase
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.UpdatePromptSource
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class RequestUpdatePromptIfNeededUseCaseTest {
    private lateinit var useCase: RequestUpdatePromptIfNeededUseCase
    private lateinit var navigator: BackStackNavigator
    private var storeChecks = 0
    private val shownPrompts = mutableListOf<Pair<UpdateAvailability.Pending, UpdatePromptSource>>()

    private val now = 1_800_000_000_000L
    private val newVersion: UpdateAvailability.Pending = UpdateAvailability.Available(
        versionName = "2.1.0",
        storeUrl = "https://apps.apple.com/app/id1",
    )

    @Test
    fun `GIVEN never prompted and a newer version WHEN opening the app THEN offers it as a startup prompt`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = null,
            availability = newVersion,
        )

        // When
        useCase()

        // Then
        assertEquals(listOf(newVersion to UpdatePromptSource.STARTUP), shownPrompts)
    }

    @Test
    fun `GIVEN never prompted and an update downloaded WHEN opening the app THEN offers to install it`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = null,
            availability = UpdateAvailability.Downloaded,
        )

        // When
        useCase()

        // Then
        assertEquals(
            listOf<UpdateAvailability.Pending>(UpdateAvailability.Downloaded),
            shownPrompts.map { it.first },
        )
        assertEquals(UpdatePromptSource.STARTUP, shownPrompts.single().second)
    }

    @Test
    fun `GIVEN a prompt just under 15 minutes ago WHEN opening the app THEN does not even ask the store`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = 15.minutes - 1.milliseconds,
            availability = UpdateAvailability.Downloaded,
        )

        // When
        useCase()

        // Then
        assertEquals(0, storeChecks)
        assertTrue(shownPrompts.isEmpty())
    }

    @Test
    fun `GIVEN a prompt 15 minutes ago and an update downloaded WHEN opening the app THEN offers to install it`() =
        runTest {
            // Given
            prepareScenario(
                lastPromptAgo = 15.minutes,
                availability = UpdateAvailability.Downloaded,
            )

            // When
            useCase()

            // Then
            assertEquals(1, storeChecks)
            assertEquals(
                listOf<UpdateAvailability.Pending>(UpdateAvailability.Downloaded),
                shownPrompts.map { it.first },
            )
        }

    @Test
    fun `GIVEN a prompt 15 minutes ago and a newer version WHEN opening the app THEN checks but offers nothing`() =
        runTest {
            // Given
            prepareScenario(
                lastPromptAgo = 15.minutes,
                availability = newVersion,
            )

            // When
            useCase()

            // Then
            assertEquals(1, storeChecks)
            assertTrue(shownPrompts.isEmpty())
        }

    @Test
    fun `GIVEN a prompt just under 1 hour ago and a newer version WHEN opening the app THEN offers nothing`() =
        runTest {
            // Given
            prepareScenario(
                lastPromptAgo = 1.hours - 1.milliseconds,
                availability = newVersion,
            )

            // When
            useCase()

            // Then
            assertEquals(1, storeChecks)
            assertTrue(shownPrompts.isEmpty())
        }

    @Test
    fun `GIVEN a prompt exactly 1 hour ago and a newer version WHEN opening the app THEN offers it`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = 1.hours,
            availability = newVersion,
        )

        // When
        useCase()

        // Then
        assertEquals(listOf(newVersion to UpdatePromptSource.STARTUP), shownPrompts)
    }

    @Test
    fun `GIVEN never prompted and no newer version WHEN opening the app THEN offers nothing`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = null,
            availability = UpdateAvailability.NotAvailable,
        )

        // When
        useCase()

        // Then
        assertEquals(1, storeChecks)
        assertTrue(shownPrompts.isEmpty())
    }

    @Test
    fun `GIVEN never prompted and the store check failing WHEN opening the app THEN offers nothing`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = null,
            availability = UpdateAvailability.CheckFailed,
        )

        // When
        useCase()

        // Then
        assertEquals(1, storeChecks)
        assertTrue(shownPrompts.isEmpty())
    }

    @Test
    fun `GIVEN the clock set back before the last prompt WHEN opening the app THEN offers the newer version`() =
        runTest {
            // Given
            prepareScenario(
                lastPromptAgo = (-5).minutes,
                availability = newVersion,
            )

            // When
            useCase()

            // Then
            assertEquals(1, storeChecks)
            assertEquals(listOf(newVersion to UpdatePromptSource.STARTUP), shownPrompts)
        }

    @Test
    fun `GIVEN the clock set back and an update downloaded WHEN opening the app THEN offers to install it`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = (-2).hours,
            availability = UpdateAvailability.Downloaded,
        )

        // When
        useCase()

        // Then
        assertEquals(
            listOf<UpdateAvailability.Pending>(UpdateAvailability.Downloaded),
            shownPrompts.map { it.first },
        )
    }

    @Test
    fun `GIVEN the web WHEN opening the app THEN does not even ask the store`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = null,
            availability = newVersion,
            mechanism = UpdateMechanism.NONE,
        )

        // When
        useCase()

        // Then
        assertEquals(0, storeChecks)
        assertTrue(shownPrompts.isEmpty())
    }

    @Test
    fun `GIVEN a group screen on top WHEN opening the app THEN offers the newer version over it`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = null,
            availability = newVersion,
            top = Route.GroupDetail(
                groupId = "g1",
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
        )

        // When
        useCase()

        // Then
        assertEquals(listOf(newVersion to UpdatePromptSource.STARTUP), shownPrompts)
    }

    @Test
    fun `GIVEN Atividade on top WHEN opening the app THEN offers the newer version over it`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = null,
            availability = newVersion,
            top = Route.Activity(personKey = null),
        )

        // When
        useCase()

        // Then
        assertEquals(listOf(newVersion to UpdatePromptSource.STARTUP), shownPrompts)
    }

    @Test
    fun `GIVEN a form on top WHEN opening the app THEN offers nothing over it`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = null,
            availability = newVersion,
            top = Route.CreateGroup,
        )

        // When
        launchUseCase()

        // Then
        assertTrue(shownPrompts.isEmpty())
    }

    @Test
    fun `GIVEN the offer waiting under a form WHEN the form closes THEN offers it on the home tab`() = runTest {
        // Given
        prepareScenario(
            lastPromptAgo = null,
            availability = newVersion,
            top = Route.CreateGroup,
        )
        launchUseCase()

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf(newVersion to UpdatePromptSource.STARTUP), shownPrompts)
    }

    private fun TestScope.launchUseCase() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase() }
    }

    private fun prepareScenario(
        lastPromptAgo: Duration?,
        availability: UpdateAvailability,
        mechanism: UpdateMechanism = UpdateMechanism.APP_STORE,
        top: Route = Route.Home,
    ) {
        val lastPromptAt = lastPromptAgo?.let { now - it.inWholeMilliseconds }
        navigator = BackStackNavigator(startRoute = Route.Home).also { if (top != Route.Home) it.navigate(top) }
        useCase = RequestUpdatePromptIfNeededUseCase(
            mechanism = mechanism,
            navigator = navigator,
            checkForUpdate = {
                storeChecks++
                availability
            },
            observeLastUpdatePromptAt = { flowOf(lastPromptAt) },
            currentTimeMillis = { now },
            showUpdatePrompt = { pending, source -> shownPrompts += pending to source },
        )
    }
}
