package com.quare.blitzsplit.core.navigation

/*
 * Why: Despesas and Membros are tabs of one group screen. Opening one from the other, or from a screen over either
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

        // Why: by the group alone, since the members just joined ("Você entrou como Bruno") are the same tab.
        below != null && below::class == tab::class -> navigateBack()

        else -> {
            navigateBack()
            replaceTop(tab)
        }
    }
}

private fun tabGroupIdOrNull(route: Route): String? = when (route) {
    is Route.GroupDetail -> route.groupId
    is Route.Members -> route.groupId
    else -> null
}
