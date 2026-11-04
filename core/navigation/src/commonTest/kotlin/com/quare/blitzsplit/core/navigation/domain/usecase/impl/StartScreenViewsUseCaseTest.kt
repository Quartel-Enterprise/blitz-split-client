package com.quare.blitzsplit.core.navigation.domain.usecase.impl

import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.navigation.BackStackNavigator
import com.quare.blitzsplit.core.navigation.Route
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StartScreenViewsUseCaseTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var navigator: BackStackNavigator
    private lateinit var startScreenViews: StartScreenViewsUseCase
    private val events = mutableListOf<Pair<String, Map<String, Any>>>()

    @Test
    fun `GIVEN Inicio on top WHEN starting THEN sends its screen view at once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(startRoute = Route.Home)

        // When
        startScreenViews()

        // Then
        assertEquals(listOf(screenView("home")), events)
    }

    @Test
    fun `GIVEN screens shown before starting WHEN starting THEN sends only the one on top`() = runTest(testDispatcher) {
        // Given
        prepareScenario(startRoute = Route.Welcome)
        navigator.navigate(Route.ProfileSetup)
        navigator.replaceAll(Route.Home)

        // When
        startScreenViews()

        // Then
        assertEquals(listOf(screenView("home")), events)
    }

    @Test
    fun `GIVEN started WHEN another screen comes on top THEN sends one view of it`() = runTest(testDispatcher) {
        // Given
        prepareScenario(startRoute = Route.Home)
        startScreenViews()

        // When
        navigator.navigate(Route.Groups)

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

        // When
        navigator.navigate(
            Route.GroupDetail(
                groupId = "group-1",
                joinedAs = "Bruno",
                tab = GroupTab.BALANCES,
            ),
        )

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
        navigator.navigate(Route.Profile)

        // When
        navigator.navigateBack()

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

            // When
            navigator.replaceAbove(
                size = 1,
                route = Route.GroupDetail(
                    groupId = "group-1",
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
            )

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

        // When
        navigator.replaceTop(
            Route.GroupDetail(
                groupId = "group-2",
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
        )

        // Then
        val groupView = screenView("group_detail", mapOf("tab" to GroupTab.EXPENSES, "is_join" to false))
        assertEquals(listOf(groupView, groupView), events)
    }

    @Test
    fun `GIVEN already started WHEN starting again THEN sends nothing more`() = runTest(testDispatcher) {
        // Given
        prepareScenario(startRoute = Route.Home)
        startScreenViews()

        // When
        startScreenViews()

        // Then
        assertEquals(listOf(screenView("home")), events)
    }

    @Test
    fun `GIVEN started twice WHEN another screen comes on top THEN sends its view once`() = runTest(testDispatcher) {
        // Given
        prepareScenario(startRoute = Route.Home)
        startScreenViews()
        startScreenViews()

        // When
        navigator.navigate(Route.Contacts)

        // Then
        assertEquals(listOf(screenView("home"), screenView("contacts")), events)
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
