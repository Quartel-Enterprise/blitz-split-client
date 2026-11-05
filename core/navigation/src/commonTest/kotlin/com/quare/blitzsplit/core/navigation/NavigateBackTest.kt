package com.quare.blitzsplit.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigateBackTest {
    private lateinit var navigator: BackStackNavigator

    @Test
    fun `GIVEN the screen on top WHEN going back from it THEN pops it`() {
        // Given
        prepareScenario(Route.Groups, Route.Profile)

        // When
        navigator.navigateBackFrom(Route.Profile)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the screen already closed WHEN going back from it again THEN the screen under it stays`() {
        // Given
        prepareScenario(Route.Groups, Route.Profile)
        navigator.navigateBackFrom(Route.Profile)

        // When
        navigator.navigateBackFrom(Route.Profile)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    private fun prepareScenario(vararg above: Route) {
        navigator = BackStackNavigator(startRoute = Route.Home).also { navigator ->
            above.forEach(navigator::navigate)
        }
    }
}
