package com.quare.blitzsplit.core.navigation

import com.quare.blitzsplit.core.model.GroupTab

/*
 * Why: a screen over the group's ("Convidar") opens the group on one of its tabs. It lands in that screen's place, so
 * the group never stacks over itself and back goes to whatever was under it: the group right below takes it as a new
 * entry on that tab, unless it already opened there. Only while from is still on top: a native back may have closed it.
 */
fun Navigator.openGroup(
    from: Route,
    groupId: String,
    tab: GroupTab,
) {
    val stack = backStack.value
    if (stack.lastOrNull() != from) return
    val below = stack.getOrNull(stack.lastIndex - 1)
    val group = Route.GroupDetail(
        groupId,
        joinedAs = null,
        tab = tab,
    )
    when {
        below == null || groupScreenIdOrNull(below) != groupId -> replaceTop(group)

        // Why: by the group and tab alone, since the expenses just joined ("Você entrou como Bruno") are the same screen.
        below is Route.GroupDetail && below.tab == tab -> navigateBack()

        else -> {
            navigateBack()
            replaceTop(group)
        }
    }
}

// Why: the group whose screen the route is, null for any other screen.
fun groupScreenIdOrNull(route: Route): String? = (route as? Route.GroupDetail)?.groupId
