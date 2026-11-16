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
        // Why: the group opened over Rachar conta stays its own entry on another tab, never equal to the one under it.
        isOverBill = below is Route.GroupDetail && below.groupId == groupId && below.isOverBill,
    )
    when {
        below == null || groupScreenIdOrNull(below) != groupId -> replaceTop(group)

        // Why: by the group and tab alone, since the expenses just joined ("Você entrou como Bruno") are the same screen.
        below is Route.GroupDetail && below.tab == tab -> navigateBack()

        // Why: in one step, so the group below is never on top in between, for a screen that watches the back stack.
        else -> replaceAbove(
            size = stack.size - 2,
            route = group,
        )
    }
}

/*
 * Why: what a group's screen (from, any of its tabs) opens: over it while on top, and in the place of the group's own
 * bill right above it, which the Mac draws beside the group (its inspector), so the group still takes clicks there.
 * The profile goes over that bill instead, since the Mac opens it in Ajustes and leaves the bill where it was. Under
 * anything else nothing opens: a native back may have closed the group. Says whether it opened, for what counts a
 * tap only when it did.
 */
fun Navigator.openFromGroupScreen(
    from: Route,
    groupId: String,
    next: Route,
): Boolean {
    val stack = backStack.value
    val top = stack.lastOrNull()
    val isUnderItsBill = top is Route.BillDetail &&
        top.groupId == groupId &&
        stack.getOrNull(stack.lastIndex - 1) == from
    return when {
        top == from || (isUnderItsBill && next == Route.Profile) -> {
            navigate(next)
            true
        }

        isUnderItsBill -> {
            replaceTop(next)
            true
        }

        else -> false
    }
}

/*
 * Why: a bill Início or "Todas as despesas" (from) opens right over it, and the Mac draws that bill beside it (its
 * inspector), so from still takes clicks there: another bill tapped takes the bill's place, as from a group's screen.
 * Under anything else nothing opens: a native back may have closed from.
 */
fun Navigator.openBillFrom(
    from: Route,
    bill: Route.BillDetail,
) {
    val stack = backStack.value
    val top = stack.lastOrNull()
    when {
        top == from -> navigate(bill)
        top is Route.BillDetail && stack.getOrNull(stack.lastIndex - 1) == from -> replaceTop(bill)
    }
}

/*
 * Why: "Esta conta foi apagada" waits for the bill's group to come back on top, so it is only posted when that group is
 * under the screen on top; Início and "Todas as despesas" open a bill with no group under it, and a notice left there
 * would show out of context the next time that group opens. "Todas as despesas" opened over the group (its "Ver tudo")
 * hides the group from what is above it: only the routes over the last one count.
 */
fun Navigator.isOverGroupScreen(groupId: String): Boolean = backStack.value
    .dropLast(1)
    .takeLastWhile { it !is Route.AllExpenses }
    .any { groupScreenIdOrNull(it) == groupId }

// Why: the group whose screen the route is, null for any other screen.
fun groupScreenIdOrNull(route: Route): String? = (route as? Route.GroupDetail)?.groupId
