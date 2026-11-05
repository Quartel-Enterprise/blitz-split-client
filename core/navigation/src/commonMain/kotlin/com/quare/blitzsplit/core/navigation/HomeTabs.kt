package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.HomeTab

/*
 * Why: Início is the root of the home, where the app opens; every other tab of the home (Grupos, Saldos, Contatos) is
 * a route right above it, and the tabs take each other's place there, as a tab bar does. Opening a
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
 * Why: Atividade, the bell of Início, from somewhere deeper (a group's notice that debts were simplified, a push
 * notification): what is above Início closes first, as a tab opening does, so back from it returns to Início and no
 * platform draws it inside a sheet. Already open, it goes back to it.
 */
fun Navigator.openActivity() {
    openHomeTab(Route.Activity(personKey = null))
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

/*
 * Why: whether a screen sits right above a home tab, so its back returns to that tab, which the web's back then names;
 * a profile opened from a bill or a group goes back there instead.
 */
fun List<Route>.isRightAboveHomeTab(route: Route): Boolean {
    val index = lastIndexOf(route)
    return index > 0 && this[index - 1].homeTabOrNull() != null
}

/*
 * Why: "Buscar" opens over the tab it was asked from only while that tab is on top, so ⌘K over a dialog on the web
 * or a late tap over a screen the tab opened never covers it. Says whether it opened, which Analytics counts.
 */
fun Navigator.openSearchOver(tab: HomeTab): Boolean {
    if (backStack.value.lastOrNull()?.homeTabOrNull() != tab) return false
    navigate(Route.Search)
    return true
}

/*
 * Why: "Buscar" from a screen outside the tabs (the web's pages, Android's rail beside a group), the navigation's item
 * there too: back to the tab the screen on top sits in, then the search over it, as iOS's search tab leaves a pushed
 * screen. Over Grupos, Grupos takes the route for its own search. Says whether it opened, which Analytics counts.
 */
fun Navigator.openSearchFromPage(): Boolean {
    val stack = backStack.value
    val top = stack.lastOrNull() ?: return false
    if (Route.Home !in stack) return false
    val tab = stack.findHomeTabUnder(top)
    openHomeTab(tab)
    return openSearchOver(tab)
}

// Why: one of the home's tabs (Início, Grupos, Saldos, Contatos), as opposed to what they opened.
fun Route.isHomeTab(): Boolean = homeTabOrNull() != null

private val HomeTab.route: Route
    get() = when (this) {
        HomeTab.HOME -> Route.Home
        HomeTab.GROUPS -> Route.Groups
        HomeTab.BALANCES -> Route.GlobalBalances(BalancesFilter.ALL, charge = null)
        HomeTab.CONTACTS -> Route.Contacts
    }

private fun Route.homeTabOrNull(): HomeTab? = when (this) {
    Route.Home -> HomeTab.HOME
    Route.Groups -> HomeTab.GROUPS
    is Route.GlobalBalances -> HomeTab.BALANCES
    Route.Contacts -> HomeTab.CONTACTS
    else -> null
}
