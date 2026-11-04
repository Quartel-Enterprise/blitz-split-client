package com.quare.blitzsplit.core.inappupdate.domain.usecase

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateMechanism
import com.quare.blitzsplit.core.inappupdate.domain.usecase.impl.ShowUpdatePromptUseCase
import com.quare.blitzsplit.core.model.UpdatePromptSource
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ShowUpdatePromptUseCaseTest {
    private lateinit var navigator: BackStackNavigator
    private lateinit var useCase: ShowUpdatePromptUseCase

    // Why: every collaborator writes here, so a test can check what happened and in which order.
    private val calls = mutableListOf<String>()
    private val storedPromptTimes = mutableListOf<Long>()

    private val now = 1_800_000_000_000L
    private val newVersion = UpdateAvailability.Available(
        versionName = "2.1.0",
        storeUrl = "https://apps.apple.com/app/id1",
    )

    @Test
    fun `GIVEN Google Play and a newer version WHEN prompting THEN stores the time before starting the update`() =
        runTest {
            // Given
            prepareScenario(mechanism = UpdateMechanism.GOOGLE_PLAY)

            // When
            useCase(newVersion, UpdatePromptSource.STARTUP)

            // Then
            assertEquals(listOf(now), storedPromptTimes)
            assertEquals(listOf(STORE_TIME, START_UPDATE), calls)
        }

    @Test
    fun `GIVEN Google Play and a newer version WHEN checking by hand THEN starts the flow without the sheet`() =
        runTest {
            // Given
            prepareScenario(mechanism = UpdateMechanism.GOOGLE_PLAY)

            // When
            useCase(newVersion, UpdatePromptSource.MANUAL)

            // Then
            assertEquals(1, calls.count { it == START_UPDATE })
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN Google Play and an update downloaded WHEN prompting THEN installs it without a new update`() = runTest {
        // Given
        prepareScenario(mechanism = UpdateMechanism.GOOGLE_PLAY)

        // When
        useCase(UpdateAvailability.Downloaded, UpdatePromptSource.STARTUP)

        // Then
        assertEquals(listOf(STORE_TIME, INSTALL), calls)
        assertEquals(listOf(now), storedPromptTimes)
    }

    @Test
    fun `GIVEN the App Store and an update downloaded WHEN prompting THEN installs it without the sheet`() = runTest {
        // Given
        prepareScenario(mechanism = UpdateMechanism.APP_STORE)

        // When
        useCase(UpdateAvailability.Downloaded, UpdatePromptSource.STARTUP)

        // Then
        assertEquals(listOf(STORE_TIME, INSTALL), calls)
        assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the App Store and a newer version WHEN prompting THEN opens the sheet with version and page`() =
        runTest {
            // Given
            prepareScenario(mechanism = UpdateMechanism.APP_STORE)

            // When
            useCase(newVersion, UpdatePromptSource.STARTUP)

            // Then
            val sheet = Route.InAppUpdate(
                versionName = "2.1.0",
                storeUrl = "https://apps.apple.com/app/id1",
                source = UpdatePromptSource.STARTUP,
            )
            assertEquals(listOf(Route.Home, sheet), navigator.backStack.value)
            assertEquals(listOf(STORE_TIME), calls)
            assertEquals(listOf(now), storedPromptTimes)
        }

    @Test
    fun `GIVEN the App Store and a newer version without a page WHEN prompting THEN opens nothing`() = runTest {
        // Given
        prepareScenario(mechanism = UpdateMechanism.APP_STORE)
        val withoutPage = UpdateAvailability.Available(
            versionName = "2.1.0",
            storeUrl = null,
        )

        // When
        useCase(withoutPage, UpdatePromptSource.MANUAL)

        // Then
        assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
        assertEquals(listOf(STORE_TIME), calls)
    }

    @Test
    fun `GIVEN the update sheet already open WHEN checking by hand THEN opens no second sheet over it`() = runTest {
        // Given
        prepareScenario(mechanism = UpdateMechanism.APP_STORE)
        val openSheet = Route.InAppUpdate(
            versionName = "2.0.5",
            storeUrl = "https://apps.apple.com/app/id1",
            source = UpdatePromptSource.STARTUP,
        )
        navigator.navigate(openSheet)

        // When
        useCase(newVersion, UpdatePromptSource.MANUAL)

        // Then
        assertEquals(listOf(Route.Home, openSheet), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the web WHEN showing the prompt for a newer version THEN only stores the time`() = runTest {
        // Given
        prepareScenario(mechanism = UpdateMechanism.NONE)

        // When
        useCase(newVersion, UpdatePromptSource.MANUAL)

        // Then
        assertEquals(listOf(STORE_TIME), calls)
        assertEquals(listOf(now), storedPromptTimes)
        assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
    }

    private fun prepareScenario(mechanism: UpdateMechanism) {
        navigator = BackStackNavigator(startRoute = Route.Home)
        useCase = ShowUpdatePromptUseCase(
            mechanism = mechanism,
            startUpdate = { calls += START_UPDATE },
            completeUpdateInstall = { calls += INSTALL },
            navigator = navigator,
            updateLastUpdatePromptAt = { epochMillis ->
                calls += STORE_TIME
                storedPromptTimes += epochMillis
            },
            currentTimeMillis = { now },
        )
    }

    private companion object {
        const val STORE_TIME = "store time"
        const val START_UPDATE = "start update"
        const val INSTALL = "install"
    }
}
