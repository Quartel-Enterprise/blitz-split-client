package com.quare.blitzsplit.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class BackStackNavigatorTest {
    private lateinit var navigator: BackStackNavigator

    @Test
    fun `GIVEN the start route WHEN navigating THEN pushes the route on top`() {
        // Given
        prepareScenario(startRoute = Route.Welcome)

        // When
        navigator.navigate(Route.ProfileSetup)

        // Then
        assertEquals(listOf(Route.Welcome, Route.ProfileSetup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a route on top WHEN navigating to it again THEN does not push it twice`() {
        // Given
        prepareScenario(startRoute = Route.Groups)
        navigator.navigate(Route.Profile)

        // When
        navigator.navigate(Route.Profile)

        // Then
        assertEquals(listOf(Route.Groups, Route.Profile), navigator.backStack.value)
    }

    @Test
    fun `GIVEN only the start route WHEN navigating back THEN keeps it`() {
        // Given
        prepareScenario(startRoute = Route.Welcome)

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf(Route.Welcome), navigator.backStack.value)
    }

    @Test
    fun `GIVEN two routes WHEN navigating back THEN drops the top one`() {
        // Given
        prepareScenario(startRoute = Route.Welcome)
        navigator.navigate(Route.Restore)

        // When
        navigator.navigateBack()

        // Then
        assertEquals(listOf(Route.Welcome), navigator.backStack.value)
    }

    @Test
    fun `GIVEN two routes WHEN replacing the top THEN puts the new route in its place`() {
        // Given
        prepareScenario(startRoute = Route.Groups)
        navigator.navigate(Route.CreateGroup)

        // When
        navigator.replaceTop(
            Route.Invite(
                groupId = "group-1",
                isNewGroup = false,
            ),
        )

        // Then
        assertEquals(
            listOf(
                Route.Groups,
                Route.Invite(
                    groupId = "group-1",
                    isNewGroup = false,
                ),
            ),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a deep stack WHEN replacing all THEN leaves only the new route`() {
        // Given
        prepareScenario(startRoute = Route.Welcome)
        navigator.navigate(Route.ProfileSetup)

        // When
        navigator.replaceAll(Route.Groups)

        // Then
        assertEquals(listOf(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN three routes WHEN popping to one THEN keeps the first`() {
        // Given
        prepareScenario(startRoute = Route.Welcome)
        navigator.navigate(Route.Restore)
        navigator.navigate(Route.ProfileSetup)

        // When
        navigator.popTo(size = 1)

        // Then
        assertEquals(listOf(Route.Welcome), navigator.backStack.value)
    }

    @Test
    fun `GIVEN two routes WHEN popping to zero THEN never empties the stack`() {
        // Given
        prepareScenario(startRoute = Route.Welcome)
        navigator.navigate(Route.Restore)

        // When
        navigator.popTo(size = 0)

        // Then
        assertEquals(listOf(Route.Welcome), navigator.backStack.value)
    }

    private fun prepareScenario(startRoute: Route) {
        navigator = BackStackNavigator(startRoute = startRoute)
    }
}
