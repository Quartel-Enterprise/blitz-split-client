package com.quare.blitzsplit.core.navigation

/*
 * Why: a wide window draws the home's Grupos as a list with the group beside it (list and detail). The group stays a
 * route right above the groups, so one back stack still drives every platform: each UI only decides, from its window,
 * whether it draws that route in the pane beside the list or over it, as a phone does.
 */

// Why: the group whose tab (Despesas, Saldos or Membros) is right above the groups, which the pane beside them shows.
fun List<Route>.groupBesideGroupsOrNull(): String? {
    val groupsIndex = indexOf(Route.Groups)
    if (groupsIndex < 0) return null
    return getOrNull(groupsIndex + 1)?.let(::tabGroupIdOrNull)
}

// Why: nothing but the pane is above the groups, so the list beside it is in sight and can be tapped.
fun List<Route>.showsOnlyPaneAboveGroups(): Boolean {
    val groupsIndex = indexOf(Route.Groups)
    return groupsIndex >= 0 && groupsIndex == lastIndex - 1 && tabGroupIdOrNull(last()) != null
}

/*
 * Why: whatever is above the groups gives way to route, so a group tapped beside another takes its place instead of
 * stacking over it. With the groups on top it is a plain push, as on a phone.
 */
fun Navigator.openBesideGroups(route: Route) {
    val groupsIndex = backStack.value.indexOf(Route.Groups)
    if (groupsIndex < 0) navigate(route) else replaceAbove(groupsIndex + 1, route)
}
