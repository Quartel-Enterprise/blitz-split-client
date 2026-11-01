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
 * Why: Atividade is a home tab over the groups, like Saldos: it takes the place of whatever the groups opened, and a
 * second tap on it while it is on top changes nothing. From a contact's history (page 11) it opens on that person.
 */
fun Navigator.openActivityTab(personKey: String?) {
    val stack = backStack.value
    val groupsIndex = stack.indexOf(Route.Groups)
    if (groupsIndex < 0) return
    val tab = Route.Activity(personKey)
    if (stack.getOrNull(groupsIndex + 1) == tab && groupsIndex + 1 == stack.lastIndex) return
    popToGroups()
    navigate(tab)
}
