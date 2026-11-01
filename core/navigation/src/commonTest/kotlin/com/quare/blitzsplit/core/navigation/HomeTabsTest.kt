package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeTabsTest {
    private lateinit var navigator: BackStackNavigator

    @Test
    fun `GIVEN a bill opened from Grupos WHEN opening Grupos THEN closes everything above Grupos`() {
        // Given
        prepareScenario(
            Route.Home,
            Route.Groups,
            Route.GroupDetail(GROUP_ID),
            Route.BillDetail(GROUP_ID, EXPENSE_ID),
        )
        val tab = Route.Groups

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Grupos over Inicio WHEN opening Inicio THEN leaves only Inicio`() {
        // Given
        prepareScenario(Route.Home, Route.Groups)
        val tab = Route.Home

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group opened from Grupos WHEN opening Saldos THEN Saldos takes the place of Grupos and the group`() {
        // Given
        prepareScenario(Route.Home, Route.Groups, Route.GroupDetail(GROUP_ID))
        val tab = Route.GlobalBalances(BalancesFilter.ALL)

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Saldos with everyone WHEN opening Saldos filtered by Voce deve THEN the filtered one takes its place`() {
        // Given
        prepareScenario(Route.Home, Route.GlobalBalances(BalancesFilter.ALL))
        val tab = Route.GlobalBalances(BalancesFilter.YOU_OWE)

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.YOU_OWE)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Registrar pagamento over Saldos WHEN opening the same Saldos THEN closes Registrar pagamento`() {
        // Given
        prepareScenario(
            Route.Home,
            Route.GlobalBalances(BalancesFilter.ALL),
            Route.SettleUp("user:carla", GroupCurrency.BRL),
        )
        val tab = Route.GlobalBalances(BalancesFilter.ALL)

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN the profile over Inicio WHEN opening Grupos THEN Grupos takes the place of the profile`() {
        // Given
        prepareScenario(Route.Home, Route.Profile)
        val tab = Route.Groups

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN only Inicio WHEN opening Grupos THEN Grupos opens over Inicio`() {
        // Given
        prepareScenario(Route.Home)
        val tab = Route.Groups

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Grupos on top WHEN opening Grupos THEN nothing changes`() {
        // Given
        prepareScenario(Route.Home, Route.Groups)
        val tab = Route.Groups

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN only Inicio WHEN opening Inicio THEN nothing changes`() {
        // Given
        prepareScenario(Route.Home)
        val tab = Route.Home

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the restore without Inicio WHEN opening Grupos THEN nothing changes`() {
        // Given
        prepareScenario(Route.Welcome, Route.Restore)
        val tab = Route.Groups

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(listOf(Route.Welcome, Route.Restore), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the profile setup without Inicio WHEN opening Inicio THEN nothing changes`() {
        // Given
        prepareScenario(Route.Welcome, Route.ProfileSetup)
        val tab = Route.Home

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(listOf(Route.Welcome, Route.ProfileSetup), navigator.backStack.value)
    }

    @Test
    fun `GIVEN Registrar pagamento over Saldos WHEN going back to Grupos THEN Grupos takes the place of both`() {
        // Given
        prepareScenario(
            Route.Home,
            Route.GlobalBalances(BalancesFilter.ALL),
            Route.SettleUp("user:carla", GroupCurrency.BRL),
        )

        // When
        navigator.popToGroups()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group opened from Grupos WHEN going back to Grupos THEN closes the group`() {
        // Given
        prepareScenario(Route.Home, Route.Groups, Route.GroupDetail(GROUP_ID), Route.Balances(GROUP_ID))

        // When
        navigator.popToGroups()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN only Inicio WHEN going back to Grupos THEN Grupos opens over Inicio`() {
        // Given
        prepareScenario(Route.Home)

        // When
        navigator.popToGroups()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN the groups on top WHEN going back to Grupos THEN nothing changes`() {
        // Given
        prepareScenario(Route.Home, Route.Groups)

        // When
        navigator.popToGroups()

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN neither the groups nor Inicio in the back stack WHEN going back to Grupos THEN nothing changes`() {
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

    private companion object {
        const val GROUP_ID = "group-1"
        const val EXPENSE_ID = "expense-1"
    }
}
