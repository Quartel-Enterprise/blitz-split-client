package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.tabGroupIdOrNull

// Why: where "Rachar conta" and "Editar conta" go once closed, saved or deleted, only while still on top.
internal class SplitBillExits(
    private val navigator: Navigator,
    private val route: Route.SplitBill,
) {
    fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    fun leave() {
        if (isOnTop()) navigator.navigateBack()
    }

    /*
     * Why: a new bill goes back to the group's screen it was split from (a tab, or its Estatísticas' empty "Rachar
     * conta", which sits over a tab), or opens its group's Despesas in its place: from the home, or once another
     * group was picked.
     */
    fun leaveToBillGroup(groupId: String) {
        if (!isOnTop()) return
        val stack = navigator.backStack.value
        val below = stack.getOrNull(stack.lastIndex - 1)
        if (below != null && (tabGroupIdOrNull(below) == groupId || below == Route.GroupStats(groupId))) {
            navigator.navigateBack()
        } else {
            navigator.replaceTop(Route.GroupDetail(groupId))
        }
    }

    // Why: back to Despesas do grupo past "Conta", which would show a bill that is gone.
    fun leaveToGroup() {
        if (!isOnTop()) return
        val stack = navigator.backStack.value
        val detailIndex = stack.indexOfLast { it is Route.BillDetail && it.expenseId == route.expenseId }
        if (detailIndex > 0) navigator.popTo(detailIndex) else navigator.navigateBack()
    }
}
