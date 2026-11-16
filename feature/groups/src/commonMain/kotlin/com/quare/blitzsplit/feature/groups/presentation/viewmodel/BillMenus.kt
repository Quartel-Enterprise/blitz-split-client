package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.billmenu.BillMenuController
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuOption
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuState
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuStep
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuUseCases
import com.quare.blitzsplit.core.expenses.billmenu.navigatingBillMenuControllerOf
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.groups.presentation.model.BillMenuEvent
import kotlinx.coroutines.CoroutineScope

// Why: a bill's ⋮ on Início and "Todas as despesas", whose options open over the list itself, with the list's source.
internal fun billMenuControllerOf(
    useCases: BillMenuUseCases,
    scope: CoroutineScope,
    navigator: Navigator,
    isOnTop: () -> Boolean,
    trackEvent: TrackEvent,
    source: String,
    showDeleteError: suspend () -> Unit,
    // Why: where the screen keeps "Apagar esta conta?", copied from the controller each time it changes.
    show: (BillMenuState) -> Unit,
): BillMenuController = navigatingBillMenuControllerOf(
    useCases = useCases,
    scope = scope,
    navigator = navigator,
    isOnTop = isOnTop,
    track = { step -> trackBillMenuStep(trackEvent, step, mapOf("source" to source)) },
    showDeleteError = showDeleteError,
    show = show,
)

private fun trackBillMenuStep(
    trackEvent: TrackEvent,
    step: BillMenuStep,
    params: Map<String, Any>,
) {
    when (step) {
        BillMenuStep.EDIT_OPENED -> trackEvent("bill_edit_clicked", params)
        BillMenuStep.DUPLICATE_OPENED -> trackEvent("bill_duplicate_clicked", params)
        BillMenuStep.SHARE_OPENED -> trackEvent("bill_share_clicked", params)
        BillMenuStep.DELETE_ASKED -> trackEvent("bill_delete_clicked", params)
        BillMenuStep.DELETE_CONFIRMED -> trackEvent("bill_delete_confirmed", params)
        BillMenuStep.DELETE_CANCELLED -> trackEvent("bill_delete_cancelled", params)
    }
}

// Why: Início's and "Todas as despesas"' ⋮ name the bill's group, since their bills come from every group.
internal fun BillMenuController.onEvent(event: BillMenuEvent) {
    when (event) {
        is BillMenuEvent.OnEditClicked -> open(BillMenuOption.EDIT, event.groupId, event.expenseId)
        is BillMenuEvent.OnDuplicateClicked -> open(BillMenuOption.DUPLICATE, event.groupId, event.expenseId)
        is BillMenuEvent.OnShareClicked -> open(BillMenuOption.SHARE, event.groupId, event.expenseId)
        is BillMenuEvent.OnDeleteClicked -> askToDelete(event.groupId, event.expenseId)
        BillMenuEvent.OnDeleteConfirmed -> confirmDelete()
        BillMenuEvent.OnDeleteDismissed -> dismissDelete()
    }
}
