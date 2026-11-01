package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.BalancesFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomePaneTest {
    private lateinit var navigator: BackStackNavigator

    @Test
    fun `GIVEN the groups on top WHEN opening a group beside them THEN pushes it`() {
        // Given
        prepareScenario(Route.Groups)

        // When
        navigator.openBesideGroups(Route.GroupDetail("trip"))

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail("trip")), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a tab of a group beside the groups WHEN opening another group THEN takes its place`() {
        // Given
        prepareScenario(Route.Groups, Route.Members("trip", joinedAs = null))

        // When
        navigator.openBesideGroups(Route.GroupDetail("flat"))

        // Then
        assertEquals(listOf(Route.Groups, Route.GroupDetail("flat")), navigator.backStack.value)
    }

    @Test
    fun `GIVEN no groups in the back stack WHEN opening a group beside them THEN pushes it`() {
        // Given
        prepareScenario(Route.Welcome)

        // When
        navigator.openBesideGroups(Route.GroupDetail("trip"))

        // Then
        assertEquals(listOf(Route.Welcome, Route.GroupDetail("trip")), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the Saldos of a group right above the groups WHEN reading the group beside them THEN is that group`() {
        // Given
        prepareScenario(Route.Groups, Route.Balances("trip"), Route.GroupStats("trip"))

        // When
        val groupId = navigator.backStack.value.groupBesideGroupsOrNull()

        // Then
        assertEquals("trip", groupId)
    }

    @Test
    fun `GIVEN Saldos right above the groups WHEN reading the group beside them THEN there is none`() {
        // Given
        prepareScenario(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL))

        // When
        val groupId = navigator.backStack.value.groupBesideGroupsOrNull()

        // Then
        assertNull(groupId)
    }

    @Test
    fun `GIVEN only a tab of a group above the groups WHEN asking whether only the pane is above THEN it is`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail("trip"))

        // When
        val showsOnlyPane = navigator.backStack.value.showsOnlyPaneAboveGroups()

        // Then
        assertTrue(showsOnlyPane)
    }

    @Test
    fun `GIVEN a bill over the group beside the groups WHEN asking whether only the pane is above THEN it is not`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail("trip"), Route.BillDetail("trip", "dinner"))

        // When
        val showsOnlyPane = navigator.backStack.value.showsOnlyPaneAboveGroups()

        // Then
        assertFalse(showsOnlyPane)
    }

    @Test
    fun `GIVEN the groups on top WHEN asking whether only the pane is above THEN it is not`() {
        // Given
        prepareScenario(Route.Groups)

        // When
        val showsOnlyPane = navigator.backStack.value.showsOnlyPaneAboveGroups()

        // Then
        assertFalse(showsOnlyPane)
    }

    private fun prepareScenario(vararg stack: Route) {
        navigator = BackStackNavigator(startRoute = stack.first()).also { navigator ->
            stack.drop(1).forEach(navigator::navigate)
        }
    }
}
