package com.quare.blitzsplit.core.expenses.billmenu

import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/*
 * Why: a bill's ⋮ on Início, "Todas as despesas" and "Expenses by category", whose options open over the list itself
 * (not over the group, as a tap on the row does), and only while the list is on top, so a late tap never stacks them
 * over what it opened. Each feature tracks the steps itself (track), so the analytics catalog finds the trackEvent
 * call next to the Manual that declares it.
 */
fun navigatingBillMenuControllerOf(
    useCases: BillMenuUseCases,
    scope: CoroutineScope,
    navigator: Navigator,
    isOnTop: () -> Boolean,
    track: (BillMenuStep) -> Unit,
    showDeleteError: suspend () -> Unit,
    // Why: where the screen keeps "Apagar esta conta?", copied from the controller each time it changes.
    show: (BillMenuState) -> Unit,
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

                        BillMenuOption.SHARE -> Route.ShareBill(
                            groupId = groupId,
                            expenseId = expenseId,
                        )
                    },
                )
            }
        }
    },
    track = track,
    showDeleteError = showDeleteError,
).also { controller -> controller.state.onEach(show).launchIn(scope) }
