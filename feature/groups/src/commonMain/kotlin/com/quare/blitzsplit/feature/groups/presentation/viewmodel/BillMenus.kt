package com.quare.blitzsplit.feature.groups.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.billmenu.BillMenuController
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuOption
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuStep
import com.quare.blitzsplit.core.expenses.billmenu.BillMenuUseCases
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.groups.presentation.model.BillMenuEvent
import kotlinx.coroutines.CoroutineScope

/*
 * Why: a bill's ⋮ on Início and "Todas as despesas", whose options open over the list itself (not over the group, as
 * a tap on the row does), and only while the list is on top, so a late tap never stacks them over what it opened.
 */
internal fun billMenuControllerOf(
    useCases: BillMenuUseCases,
    scope: CoroutineScope,
    navigator: Navigator,
    isOnTop: () -> Boolean,
    trackEvent: TrackEvent,
    source: String,
    showDeleteError: suspend () -> Unit,
): BillMenuController = BillMenuController(
    useCases = useCases,
    scope = scope,
    openOption = { option, groupId, expenseId ->
        isOnTop().also { canOpen ->
            if (canOpen) {
                navigator.navigate(
                    when (option) {
                        BillMenuOption.EDIT -> Route.SplitBill.createEditing(groupId, expenseId)
                        BillMenuOption.DUPLICATE -> Route.SplitBill.createDuplicating(groupId, expenseId)
                        BillMenuOption.SHARE -> Route.ShareBill(groupId = groupId, expenseId = expenseId)
                    },
                )
            }
        }
    },
    track = { step -> trackBillMenuStep(trackEvent, step, mapOf("source" to source)) },
    showDeleteError = showDeleteError,
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
