package com.quare.blitzsplit.feature.inappupdate

import com.quare.blitzsplit.core.model.UpdatePromptSource
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.feature.inappupdate.presentation.model.InAppUpdateUiAction
import com.quare.blitzsplit.feature.inappupdate.presentation.model.InAppUpdateUiEvent
import com.quare.blitzsplit.feature.inappupdate.presentation.viewmodel.InAppUpdateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InAppUpdateViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var navigator: BackStackNavigator
    private lateinit var route: Route.InAppUpdate
    private lateinit var viewModel: InAppUpdateViewModel
    private val trackedEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    private val actions = mutableListOf<InAppUpdateUiAction>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN the store says the new version WHEN the sheet opens THEN shows that version`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(versionName = "2.1.0")

            // When
            val state = viewModel.uiState.value

            // Then
            assertEquals("2.1.0", state.versionName)
        }

    @Test
    fun `GIVEN the store does not say the version WHEN the sheet opens THEN shows no version`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(versionName = null)

            // When
            val state = viewModel.uiState.value

            // Then
            assertNull(state.versionName)
        }

    @Test
    fun `GIVEN the store says the version WHEN the sheet opens THEN tracks the prompt with its source and version`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                versionName = "2.1.0",
                source = UpdatePromptSource.STARTUP,
            )

            // When
            val events = trackedEvents.toList()

            // Then
            assertEquals(
                listOf(
                    "update_prompt_shown" to
                        mapOf<String, Any>("source" to UpdatePromptSource.STARTUP, "version" to "2.1.0"),
                ),
                events,
            )
        }

    @Test
    fun `GIVEN the store does not say the version WHEN the sheet opens THEN tracks the prompt with its source only`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                versionName = null,
                source = UpdatePromptSource.MANUAL,
            )

            // When
            val events = trackedEvents.toList()

            // Then
            assertEquals(
                listOf("update_prompt_shown" to mapOf<String, Any>("source" to UpdatePromptSource.MANUAL)),
                events,
            )
        }

    @Test
    fun `GIVEN the update sheet WHEN tapping update THEN tracks the update as accepted with its source`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(source = UpdatePromptSource.MANUAL)
            trackedEvents.clear()

            // When
            viewModel.onEvent(InAppUpdateUiEvent.OnUpdateClicked)

            // Then
            assertEquals(
                listOf("update_accepted" to mapOf<String, Any>("source" to UpdatePromptSource.MANUAL)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the update sheet WHEN tapping update THEN opens the store page of the app`() = runTest(testDispatcher) {
        // Given
        prepareScenario(storeUrl = "https://apps.apple.com/app/id123")

        // When
        viewModel.onEvent(InAppUpdateUiEvent.OnUpdateClicked)

        // Then
        assertEquals(
            listOf<InAppUpdateUiAction>(InAppUpdateUiAction.OpenStore("https://apps.apple.com/app/id123")),
            actions,
        )
    }

    @Test
    fun `GIVEN the update sheet over the home WHEN tapping update THEN closes the sheet back to the home`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(InAppUpdateUiEvent.OnUpdateClicked)

            // Then
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the update sheet WHEN dismissing it THEN tracks the update as dismissed with its source`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(source = UpdatePromptSource.STARTUP)
            trackedEvents.clear()

            // When
            viewModel.onEvent(InAppUpdateUiEvent.OnDismissed)

            // Then
            assertEquals(
                listOf("update_dismissed" to mapOf<String, Any>("source" to UpdatePromptSource.STARTUP)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN the update sheet over the home WHEN dismissing it THEN closes the sheet without opening the store`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario()

            // When
            viewModel.onEvent(InAppUpdateUiEvent.OnDismissed)

            // Then
            assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
            assertTrue(actions.isEmpty())
        }

    @Test
    fun `GIVEN the sheet already closed over the profile WHEN the dismiss arrives THEN keeps the profile open`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(screensUnder = listOf(Route.Home, Route.Profile))
            navigator.navigateBack()

            // When
            viewModel.onEvent(InAppUpdateUiEvent.OnDismissed)

            // Then
            assertEquals(listOf(Route.Home, Route.Profile), navigator.backStack.value)
        }

    @Test
    fun `GIVEN the sheet already closed over the profile WHEN the dismiss arrives THEN still tracks the dismiss`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(
                screensUnder = listOf(Route.Home, Route.Profile),
                source = UpdatePromptSource.MANUAL,
            )
            navigator.navigateBack()
            trackedEvents.clear()

            // When
            viewModel.onEvent(InAppUpdateUiEvent.OnDismissed)

            // Then
            assertEquals(
                listOf("update_dismissed" to mapOf<String, Any>("source" to UpdatePromptSource.MANUAL)),
                trackedEvents,
            )
        }

    @Test
    fun `GIVEN another screen opened over the sheet WHEN tapping update THEN opens the store and keeps that screen`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(storeUrl = "https://apps.apple.com/app/id123")
            navigator.navigate(Route.Profile)

            // When
            viewModel.onEvent(InAppUpdateUiEvent.OnUpdateClicked)

            // Then
            assertEquals(listOf(Route.Home, route, Route.Profile), navigator.backStack.value)
            assertEquals(
                listOf<InAppUpdateUiAction>(InAppUpdateUiAction.OpenStore("https://apps.apple.com/app/id123")),
                actions,
            )
        }

    private fun TestScope.prepareScenario(
        versionName: String? = "2.1.0",
        storeUrl: String = "https://apps.apple.com/app/id1",
        source: UpdatePromptSource = UpdatePromptSource.STARTUP,
        screensUnder: List<Route> = listOf(Route.Home),
    ) {
        route = Route.InAppUpdate(
            versionName = versionName,
            storeUrl = storeUrl,
            source = source,
        )
        navigator = BackStackNavigator(startRoute = screensUnder.first()).also { navigator ->
            screensUnder.drop(1).forEach(navigator::navigate)
            navigator.navigate(route)
        }
        viewModel = InAppUpdateViewModel(
            route = route,
            navigator = navigator,
            trackEvent = { name, params -> trackedEvents += name to params },
        )
        backgroundScope.launch { viewModel.uiAction.collect(actions::add) }
    }
}
