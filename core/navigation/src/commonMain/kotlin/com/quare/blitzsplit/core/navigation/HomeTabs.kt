package com.quare.blitzsplit.core.navigation

/*
 * Why: Início is the root of the home, where the app opens; every other tab of the home (Grupos, Saldos, Contatos,
 * Atividade) is a route right above it, and the tabs take each other's place there, as a tab bar does. Opening the tab
 * already right above Início goes back to it, closing whatever it opened and keeping its own state (Saldos' filter);
 * any other tab replaces whatever is above Início. Without Início in the back stack (the welcome, the restore) nothing
 * changes: there is no home yet.
 */
fun Navigator.openHomeTab(tab: Route) {
    val stack = backStack.value
    val homeIndex = stack.indexOf(Route.Home)
    if (homeIndex < 0) return
    val above = stack.getOrNull(homeIndex + 1)
    when {
        tab == Route.Home -> popTo(homeIndex + 1)
        above != null && above::class == tab::class -> popTo(homeIndex + 2)
        else -> replaceAbove(homeIndex + 1, tab)
    }
}

/*
 * Why: Grupos is the home tab under what a group's row opened. Going back to it closes all of them, as a tab bar
 * does; nothing changes while the groups are on top.
 */
fun Navigator.popToGroups() {
    if (backStack.value.lastOrNull() != Route.Groups) openHomeTab(Route.Groups)
}
