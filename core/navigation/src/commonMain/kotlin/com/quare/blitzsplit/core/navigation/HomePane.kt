package com.quare.blitzsplit.core.navigation

/*
 * Why: a wide window draws the home's Grupos as a list with the group beside it (list and detail). The group stays a
 * route right above the groups, so one back stack still drives every platform: each UI only decides, from its window,
 * whether it draws that route in the pane beside the list or over it, as a phone does.
 *
 * The group tabs (Despesas, Saldos or Membros) right above the groups. Another group's tab may stack on the one a
 * row opened (a bill saved in another group, Convidar's "Ir para o grupo"), and the pane shows the last of them.
 */
private fun List<Route>.findTabsAboveGroups(): List<Route> {
    val groupsIndex = indexOf(Route.Groups)
    if (groupsIndex < 0) return emptyList()
    return drop(groupsIndex + 1).takeWhile { tabGroupIdOrNull(it) != null }
}

// Why: the group whose tab the pane beside the groups shows.
fun List<Route>.groupBesideGroupsOrNull(): String? = findTabsAboveGroups().lastOrNull()?.let(::tabGroupIdOrNull)

// Why: nothing but the pane's tabs is above the groups, so the list beside it is in sight and can be tapped.
fun List<Route>.showsOnlyPaneAboveGroups(): Boolean {
    val groupsIndex = indexOf(Route.Groups)
    return groupsIndex in 0 until lastIndex && findTabsAboveGroups().size == lastIndex - groupsIndex
}

/*
 * Why: whatever is above the groups gives way to route, so a group tapped beside another takes its place instead of
 * stacking over it. With the groups on top it is a plain push, as on a phone.
 */
fun Navigator.openBesideGroups(route: Route) {
    val groupsIndex = backStack.value.indexOf(Route.Groups)
    if (groupsIndex < 0) navigate(route) else replaceAbove(groupsIndex + 1, route)
}
