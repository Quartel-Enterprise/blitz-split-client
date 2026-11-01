package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeTabsTest {
    private lateinit var navigator: BackStackNavigator

    @Test
    fun `GIVEN Registrar pagamento over Saldos WHEN going back to Grupos THEN closes both`() {
        // Given
        prepareScenario(
            Route.Groups,
            Route.GlobalBalances(BalancesFilter.ALL, chargePersonKey = null),
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

    private fun prepareScenario(vararg stack: Route) {
        navigator = BackStackNavigator(startRoute = stack.first()).also { navigator ->
            stack.drop(1).forEach(navigator::navigate)
        }
    }
}
