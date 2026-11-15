package com.quare.blitzsplit.feature.billdetail.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.usecase.NoticeBillGone
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.navigation.isOverGroupScreen

/*
 * Why: the group or the bill went while "Editar conta" covered "Conta", which then leaves as soon as it is on top
 * again. A bill that went after it showed says so on Despesas do grupo, unless this device deleted it or no group is
 * under it (Início and "Todas as despesas" open a bill straight over themselves).
 */
internal class BillDetailLeaving(
    private val navigator: Navigator,
    private val route: Route.BillDetail,
    private val noticeBillGone: NoticeBillGone,
) {
    fun isOnTop(): Boolean = navigator.backStack.value.lastOrNull() == route

    private var isGone = false
    private var isBillGoneAfterShowing = false

    fun leaveIfGone() {
        if (isGone) leave()
    }

    fun markGone(isBillGoneAfterShowing: Boolean) {
        isGone = true
        this.isBillGoneAfterShowing = isBillGoneAfterShowing
        leave()
    }

    // Why: only while it is still on top: a native back (a swipe, the browser's) may have closed it.
    fun leave() {
        if (!isOnTop()) return
        if (isBillGoneAfterShowing && navigator.isOverGroupScreen(route.groupId)) {
            noticeBillGone(route.groupId, route.expenseId)
        }
        navigator.navigateBack()
    }
}
