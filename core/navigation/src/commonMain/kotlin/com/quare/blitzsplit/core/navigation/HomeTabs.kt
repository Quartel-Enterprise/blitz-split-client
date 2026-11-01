package com.quare.blitzsplit.core.navigation

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
