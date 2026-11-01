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
