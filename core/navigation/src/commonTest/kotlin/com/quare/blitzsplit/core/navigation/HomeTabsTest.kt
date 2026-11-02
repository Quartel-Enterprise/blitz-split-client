package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab
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
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
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
        prepareScenario(
            Route.Home,
            Route.Groups,
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
        )
        val tab = Route.GlobalBalances(BalancesFilter.ALL, charge = null)

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Saldos with everyone WHEN opening Saldos filtered by Voce deve THEN the filtered one takes its place`() {
        // Given
        prepareScenario(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null))
        val tab = Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null)

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN Registrar pagamento over Saldos WHEN opening the same Saldos THEN closes Registrar pagamento`() {
        // Given
        prepareScenario(
            Route.Home,
            Route.GlobalBalances(BalancesFilter.ALL, charge = null),
            Route.SettleUp("user:carla", GroupCurrency.BRL),
        )
        val tab = Route.GlobalBalances(BalancesFilter.ALL, charge = null)

        // When
        navigator.openHomeTab(tab)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
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
            Route.GlobalBalances(BalancesFilter.ALL, charge = null),
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
        prepareScenario(
            Route.Home,
            Route.Groups,
            Route.GroupDetail(
                GROUP_ID,
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
            Route.GroupDetail(GROUP_ID, joinedAs = null, GroupTab.BALANCES),
        )

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

    @Test
    fun `GIVEN a group over the groups WHEN its rail opens Grupos THEN goes back to the groups`() {
        // Given
        prepareScenario(
            Route.Home,
            Route.Groups,
            Route.GroupDetail(
                "trip",
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
        )

        // When
        navigator.openHomeTab(HomeTab.GROUPS)

        // Then
        assertEquals(listOf(Route.Home, Route.Groups), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group over the groups WHEN its rail opens Saldos THEN Saldos takes the groups place`() {
        // Given
        prepareScenario(
            Route.Home,
            Route.Groups,
            Route.GroupDetail(
                "trip",
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
            Route.GroupDetail("trip", joinedAs = null, GroupTab.MEMBERS),
        )

        // When
        navigator.openHomeTab(HomeTab.BALANCES)

        // Then
        assertEquals(
            listOf(Route.Home, Route.GlobalBalances(BalancesFilter.ALL, charge = null)),
            navigator.backStack.value,
        )
    }

    @Test
    fun `GIVEN a group over Saldos filtered WHEN its rail opens Saldos THEN goes back to that Saldos`() {
        // Given
        val balances = Route.GlobalBalances(BalancesFilter.YOU_OWE, charge = null)
        prepareScenario(
            Route.Home,
            balances,
            Route.GroupDetail(
                "trip",
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
        )

        // When
        navigator.openHomeTab(HomeTab.BALANCES)

        // Then
        assertEquals(listOf(Route.Home, balances), navigator.backStack.value)
    }

    @Test
    fun `GIVEN statistics over the groups WHEN its rail opens Contatos THEN Contatos takes the groups place`() {
        // Given
        prepareScenario(Route.Home, Route.Groups, Route.GroupStats("trip"))

        // When
        navigator.openHomeTab(HomeTab.CONTACTS)

        // Then
        assertEquals(listOf(Route.Home, Route.Contacts), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group over the groups WHEN its rail opens Atividade THEN Atividade takes the groups place`() {
        // Given
        prepareScenario(
            Route.Home,
            Route.Groups,
            Route.GroupDetail(
                "trip",
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
        )

        // When
        navigator.openHomeTab(HomeTab.ACTIVITY)

        // Then
        assertEquals(listOf(Route.Home, Route.Activity(personKey = null)), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group Inicio opened WHEN its rail opens Inicio THEN goes back to Inicio`() {
        // Given
        prepareScenario(
            Route.Home,
            Route.GroupDetail(
                "trip",
                joinedAs = null,
                tab = GroupTab.EXPENSES,
            ),
        )

        // When
        navigator.openHomeTab(HomeTab.HOME)

        // Then
        assertEquals(listOf<Route>(Route.Home), navigator.backStack.value)
    }

    @Test
    fun `GIVEN a group over the groups WHEN asking its home tab THEN is Grupos`() {
        // Given
        val route = Route.GroupDetail("trip", joinedAs = null, GroupTab.MEMBERS)
        val stack =
            listOf(
                Route.Home,
                Route.Groups,
                Route.GroupDetail(
                    "trip",
                    joinedAs = null,
                    tab = GroupTab.EXPENSES,
                ),
                route,
            )

        // When
        val tab = stack.findHomeTabUnder(route)

        // Then
        assertEquals(HomeTab.GROUPS, tab)
    }

    @Test
    fun `GIVEN a group Inicio opened WHEN asking its home tab THEN is Inicio`() {
        // Given
        val route = Route.GroupDetail(
            "trip",
            joinedAs = null,
            tab = GroupTab.EXPENSES,
        )

        // When
        val tab = listOf(Route.Home, route).findHomeTabUnder(route)

        // Then
        assertEquals(HomeTab.HOME, tab)
    }

    @Test
    fun `GIVEN a group over a contact WHEN asking its home tab THEN is Contatos`() {
        // Given
        val route = Route.GroupDetail(
            "trip",
            joinedAs = null,
            tab = GroupTab.EXPENSES,
        )
        val stack = listOf(Route.Home, Route.Contacts, Route.ContactsAgenda, route)

        // When
        val tab = stack.findHomeTabUnder(route)

        // Then
        assertEquals(HomeTab.CONTACTS, tab)
    }

    @Test
    fun `GIVEN a screen no longer in the back stack WHEN asking its home tab THEN is the top one`() {
        // Given
        val stack = listOf(Route.Home, Route.Activity(personKey = null))

        // When
        val tab = stack.findHomeTabUnder(Route.GroupStats("trip"))

        // Then
        assertEquals(HomeTab.ACTIVITY, tab)
    }

    @Test
    fun `GIVEN no home in the back stack WHEN asking a screen home tab THEN is Inicio`() {
        // Given
        val route = Route.GroupDetail(
            "trip",
            joinedAs = null,
            tab = GroupTab.EXPENSES,
        )

        // When
        val tab = listOf(Route.Welcome, route).findHomeTabUnder(route)

        // Then
        assertEquals(HomeTab.HOME, tab)
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
