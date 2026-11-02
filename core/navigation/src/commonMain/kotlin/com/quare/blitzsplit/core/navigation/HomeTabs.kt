package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.HomeTab

/*
 * Why: Início is the root of the home, where the app opens; every other tab of the home (Grupos, Saldos, Contatos,
 * Atividade) is a route right above it, and the tabs take each other's place there, as a tab bar does. Opening a
 * tab already in the back stack goes back to it, closing whatever it opened; any other tab, Saldos with another
 * filter included, replaces whatever is above Início. Without Início in the back stack (the welcome, the restore)
 * nothing changes: there is no home yet.
 */
fun Navigator.openHomeTab(tab: Route) {
    val stack = backStack.value
    val tabIndex = stack.indexOf(tab)
    val homeIndex = stack.indexOf(Route.Home)
    when {
        tabIndex >= 0 -> popTo(tabIndex + 1)
        homeIndex >= 0 -> replaceAbove(homeIndex + 1, tab)
    }
}

/*
 * Why: Grupos is the home tab under what a group's row opened. Going back to it closes all of them, as a tab bar
 * does.
 */
fun Navigator.popToGroups() {
    openHomeTab(Route.Groups)
}

/*
 * Why: a tab of the rail a screen outside the home keeps (Android's beside a group's screens drawn alone), through
 * openHomeTab like the home's own tabs: a tab already in the back stack, Saldos with any filter, goes back to it, and
 * any other opens in its first state.
 */
fun Navigator.openHomeTab(tab: HomeTab) {
    openHomeTab(backStack.value.lastOrNull { it.homeTabOrNull() == tab } ?: tab.route)
}

/*
 * Why: the home tab a screen outside the home sits in, which the rail beside it selects: the nearest one under it in
 * the back stack (Início for a group Início opened, Grupos for one a row of Grupos opened), Início once none is.
 */
fun List<Route>.findHomeTabUnder(route: Route): HomeTab {
    val index = lastIndexOf(route).takeIf { it >= 0 } ?: size
    return take(index).asReversed().firstNotNullOfOrNull { it.homeTabOrNull() } ?: HomeTab.HOME
}

private val HomeTab.route: Route
    get() = when (this) {
        HomeTab.HOME -> Route.Home
        HomeTab.GROUPS -> Route.Groups
        HomeTab.BALANCES -> Route.GlobalBalances(BalancesFilter.ALL, charge = null)
        HomeTab.CONTACTS -> Route.Contacts
        HomeTab.ACTIVITY -> Route.Activity(personKey = null)
    }

private fun Route.homeTabOrNull(): HomeTab? = when (this) {
    Route.Home -> HomeTab.HOME
    Route.Groups -> HomeTab.GROUPS
    is Route.GlobalBalances -> HomeTab.BALANCES
    Route.Contacts -> HomeTab.CONTACTS
    is Route.Activity -> HomeTab.ACTIVITY
    else -> null
}
