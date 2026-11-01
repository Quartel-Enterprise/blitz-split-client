package com.quare.blitzsplit.core.navigation

/*
 * Why: Grupos is the home's root tab, under Saldos and whatever either opened. Going back to it closes all of them, as
 * a tab bar does; nothing changes while the groups are on top or gone from the back stack.
 */
fun Navigator.popToGroups() {
    val stack = backStack.value
    val groupsIndex = stack.indexOf(Route.Groups)
    if (groupsIndex >= 0 && groupsIndex < stack.lastIndex) popTo(groupsIndex + 1)
}

/*
 * Why: Saldos and Contatos are tabs next to Grupos, drawn over the groups; from a screen the groups or another tab
 * opened, the tab closes it first, as a tab bar does. Nothing changes while that tab is already right above the groups.
 */
fun Navigator.openHomeTab(route: Route) {
    val stack = backStack.value
    val groupsIndex = stack.indexOf(Route.Groups)
    if (groupsIndex < 0) return
    val above = stack.getOrNull(groupsIndex + 1)
    if (above != null && above::class == route::class && groupsIndex + 1 == stack.lastIndex) return
    popToGroups()
    navigate(route)
}
