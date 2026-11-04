package com.quare.blitzsplit.core.navigation.domain.usecase.impl

import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class StartScreenViewsUseCaseTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var navigator: BackStackNavigator
    private lateinit var startScreenViews: StartScreenViewsUseCase
    private val events = mutableListOf<Pair<String, Map<String, Any>>>()

    @Test
    fun `GIVEN Inicio on top WHEN starting THEN sends its screen view once it stays on top`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(startRoute = Route.Home)

            stay()

            // When
            startScreenViews()

            stay()

            // Then
            assertEquals(listOf(screenView("home")), events)
        }

    @Test
    fun `GIVEN screens shown before starting WHEN starting THEN sends only the one on top`() = runTest(testDispatcher) {
        // Given
        prepareScenario(startRoute = Route.Welcome)
        navigator.navigate(Route.ProfileSetup)
        navigator.replaceAll(Route.Home)

        stay()

        // When
        startScreenViews()

        stay()

        // Then
        assertEquals(listOf(screenView("home")), events)
    }

    @Test
    fun `GIVEN started WHEN another screen comes on top THEN sends one view of it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(startRoute = Route.Home)
        startScreenViews()

        stay()

        // When
        navigator.navigate(Route.Groups)

        stay()

        // Then
        assertEquals(listOf(screenView("home"), screenView("groups")), events)
    }

    @Test
    fun `GIVEN started WHEN a group opens THEN sends its view with the tab and how it was opened`() = runTest(
        testDispatcher,
    ) {
        // Given
        prepareScenario(startRoute = Route.Groups)
        startScreenViews()

        stay()

        // When
        navigator.navigate(
            Route.GroupDetail(
                groupId = "group-1",
                joinedAs = "Bruno",
                tab = GroupTab.BALANCES,
            ),
        )

        stay()

        // Then
        assertEquals(
            listOf(
                screenView("groups"),
                screenView("group_detail", mapOf("tab" to GroupTab.BALANCES, "is_join" to true)),
            ),
            events,
        )
    }

    @Test
    fun `GIVEN a screen opened over Inicio WHEN going back THEN sends a view of Inicio again`() = runTest(
        testDispatcher,
    ) {
        // Given
        prepareScenario(startRoute = Route.Home)
        startScreenViews()
        stay()
        navigator.navigate(Route.Profile)

        stay()

        // When
        navigator.navigateBack()

        stay()

        // Then
        assertEquals(listOf(screenView("home"), screenView("profile"), screenView("home")), events)
    }

    @Test
    fun `GIVEN a group on top WHEN the screens under it change and the same group stays on top THEN sends nothing`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(startRoute = Route.Home)
            navigator.navigate(Route.Groups)
            navigator.navigate(
                Route.GroupDetail(
                    groupId = "group-1",
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            )
            startScreenViews()

            stay()

            // When
            navigator.replaceAbove(
                size = 1,
                route = Route.GroupDetail(
                    groupId = "group-1",
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            )

            stay()

            // Then
            assertEquals(
                listOf(screenView("group_detail", mapOf("tab" to GroupTab.EXPENSES, "is_join" to false))),
                events,
            )
        }

    @Test
    fun `GIVEN a group on top WHEN another group takes its place THEN sends another view`() = runTest(testDispatcher) {
        // Given
        prepareScenario(startRoute = Route.Groups)
        navigator.navigate(
            Route.GroupDetail(
                groupId = "group-1",
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
        )
        startScreenViews()

        stay()

        // When
        navigator.replaceTop(
            Route.GroupDetail(
                groupId = "group-2",
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
        )

        stay()

        // Then
        val groupView = screenView("group_detail", mapOf("tab" to GroupTab.EXPENSES, "is_join" to false))
        assertEquals(listOf(groupView, groupView), events)
    }

    @Test
    fun `GIVEN already started WHEN starting again THEN sends nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario(startRoute = Route.Home)
        startScreenViews()

        stay()

        // When
        startScreenViews()

        stay()

        // Then
        assertEquals(listOf(screenView("home")), events)
    }

    @Test
    fun `GIVEN started twice WHEN another screen comes on top THEN sends its view once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(startRoute = Route.Home)
        startScreenViews()
        startScreenViews()

        stay()

        // When
        navigator.navigate(Route.Contacts)

        stay()

        // Then
        assertEquals(listOf(screenView("home"), screenView("contacts")), events)
    }

    @Test
    fun `GIVEN started WHEN a screen comes on top and goes at once THEN sends nothing more`() =
        runTest(testDispatcher) {
            // Given
            prepareScenario(startRoute = Route.Home)
            startScreenViews()
            stay()

            // When
            navigator.navigate(Route.Profile)
            navigator.navigateBack()
            stay()

            // Then
            assertEquals(listOf(screenView("home")), events)
        }

    // Why: a screen on top as long as one counts.
    private fun TestScope.stay() {
        advanceTimeBy(screenStay + 1.milliseconds)
    }

    // Why: GA4's screen_view, the screen's name as both its name and class, beside the route's own params.
    private fun screenView(
        name: String,
        params: Map<String, Any> = emptyMap(),
    ): Pair<String, Map<String, Any>> = "screen_view" to params + mapOf("screen_name" to name, "screen_class" to name)

    private fun TestScope.prepareScenario(startRoute: Route) {
        navigator = BackStackNavigator(startRoute = startRoute)
        startScreenViews = StartScreenViewsUseCase(
            navigator = navigator,
            trackEvent = { name, params -> events += name to params },
            scope = backgroundScope,
        )
    }
}
