package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.HomeTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeTabsTest {
    private lateinit var navigator: BackStackNavigator

    @Test
    fun `GIVEN Registrar pagamento over Saldos WHEN going back to Grupos THEN closes both`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.GlobalBalances(BalancesFilter.ALL, charge = null),
            Route.SettleUp("user:carla", GroupCurrency.BRL),
        )

        // When
        navigator.popToGroups()

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the groups on top WHEN going back to Grupos THEN nothing changes`() {
        // Given
        prepareScenario(Route.Groups)

        // When
        navigator.popToGroups()

        // Then
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN no groups in the back stack WHEN going back to Grupos THEN nothing changes`() {
        // Given
        prepareScenario(Route.Welcome, Route.Restore)

        // When
        navigator.popToGroups()

        // Then
        assertEquals(listOf(Route.Welcome, Route.Restore), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group over the groups WHEN its rail opens Grupos THEN goes back to the groups`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail("trip", joinedAs = null))

        // When
        val isOpened = navigator.openHomeTab(HomeTab.GROUPS)

        // Then
        assertTrue(isOpened)
        assertEquals(listOf<Route>(Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group over the groups WHEN its rail opens Saldos THEN Saldos takes the group's place`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail("trip", joinedAs = null), Route.Members("trip"))

        // When
        val isOpened = navigator.openHomeTab(HomeTab.BALANCES)

        // Then
        assertTrue(isOpened)
        assertEquals(
            listOf(Route.Groups, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a group over the groups WHEN its rail opens Contatos THEN Contatos takes the group's place`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupStats("trip"))

        // When
        val isOpened = navigator.openHomeTab(HomeTab.CONTACTS)

        // Then
        assertTrue(isOpened)
        assertEquals(listOf(Route.Groups, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group over the groups WHEN its rail opens Atividade THEN says it isn't built and stays`() {
        // Given
        prepareScenario(Route.Groups, Route.GroupDetail("trip", joinedAs = null))

        // When
        val isOpened = navigator.openHomeTab(HomeTab.ACTIVITY)

        // Then
        assertFalse(isOpened)
        assertEquals(
            listOf(Route.Groups, Route.GroupDetail("trip", joinedAs = null)),
            navigator.backStack.value,
        )
    }

    private fun prepareScenario(vararg stack: Route) {
        navigator = BackStackNavigator(startRoute = stack.first()).also { navigator ->
            stack.drop(1).forEach(navigator::navigate)
        }
    }
}
