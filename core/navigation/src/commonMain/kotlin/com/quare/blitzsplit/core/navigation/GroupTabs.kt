package com.quare.blitzsplit.core.navigation

/*
 * Why: Despesas, Saldos and Membros are tabs of one group screen. Opening one from the other, or from a screen over either
 * ("Convidar"), lands on the tab in that screen's place, so the group never stacks over itself and back goes to
 * whatever was under it. Only while from is still on top: a native back may have closed it.
 */
fun Navigator.openGroupTab(
    from: Route,
    tab: Route,
) {
    val stack = backStack.value
    if (stack.lastOrNull() != from) return
    val below = stack.getOrNull(stack.lastIndex - 1)
    val belowGroupId = below?.let(::tabGroupIdOrNull)
    when {
        belowGroupId == null || belowGroupId != tabGroupIdOrNull(tab) -> replaceTop(tab)

        // Why: by the group alone, since the expenses just joined ("Você entrou como Bruno") are the same tab.
        below != null && below::class == tab::class -> navigateBack()

        else -> {
            navigateBack()
            replaceTop(tab)
        }
    }
}

// Why: the group whose tab (Despesas, Saldos or Membros) the route is, null for any other screen.
fun tabGroupIdOrNull(route: Route): String? = when (route) {
    is Route.GroupDetail -> route.groupId
    is Route.Balances -> route.groupId
    is Route.Members -> route.groupId
    else -> null
}

/*
 * Why: moving between a group's tabs only swaps what is under the tabs, so the UIs draw it in place, with no screen
 * transition, while the group's header stays put. Opening or leaving the group still plays one.
 */
fun isGroupTabSwitch(
    from: Route,
    to: Route,
): Boolean {
    val groupId = tabGroupIdOrNull(from) ?: return false
    return from != to && groupId == tabGroupIdOrNull(to)
}
