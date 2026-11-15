package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: "Paid with" (private payment methods): a new bill opens on the person's default method, a duplicate on its
 * original's mark, a bill being edited on its own; what they pick is saved for them alone once the bill is.
 */
internal class PaidWithPicking(
    private val uiState: MutableStateFlow<SplitBillUiState>,
    private val route: Route.SplitBill,
    private val useCases: BillPayingMethodUseCases,
    private val trackEvent: TrackEvent,
    // Why: opens "New payment method" over the form, unless another screen already covers it.
    private val exits: SplitBillExits,
    private val scope: CoroutineScope,
) {
    private var hasOpened = false

    /*
     * Why: "Add a payment method" was tapped, which only shows while the person has no method, so the first method the
     * list brings is the one it saved. Kept until the form closes, so a method synced later is never picked for them.
     */
    private var isAwaitingAdded = false

    init {
        val markedId = route.expenseId ?: route.duplicatesExpenseId
        flow { emit(markedId?.let { useCases.observeBillPayingMethod(it).first() }) }
            .flatMapLatest { mark -> useCases.observePayingMethods().map { methods -> methods to mark } }
            .onEach { (methods, mark) -> show(methods, mark) }
            .launchIn(scope)
    }

    fun pick(methodId: String?) {
        val state = uiState.value
        // Why: nothing changes while a receipt is read, whichever UI let the tap through (TalkBack included).
        if (!state.showsPaidWith || state.isFormLocked || methodId == state.paidWith.methodId) return
        if (methodId != null && state.paidWith.methods.none { it.id == methodId }) return
        trackEvent("split_bill_paid_with_picked", mapOf("has_method" to (methodId != null)))
        uiState.update { it.copy(paidWith = it.paidWith.copy(methodId = methodId)) }
    }

    // Why: "Add a payment method", only while it shows, and never while the bill saves, which leaves the form.
    fun add() {
        val state = uiState.value
        if (!state.showsAddPayingMethod || state.isFormLocked || state.isSaving || state.isDeleting) return
        val form = exits.openNewPayingMethod() ?: return
        trackEvent("split_bill_add_paying_method_clicked", emptyMap())
        isAwaitingAdded = true
        scope.launch {
            exits.awaitClosed(form)
            /*
             * Why: the form saves to Room before it closes, so a fresh read already has the method even if the list
             * hasn't told of it yet; a cancelled form finds none and stops waiting.
             */
            val methods = useCases.observePayingMethods().first()
            if (isAwaitingAdded && methods.isNotEmpty()) show(methods, mark = null)
            isAwaitingAdded = false
        }
    }

    /*
     * Why: once the bill is saved: the method picked while the person pays it, or none once they no longer do. A bill
     * they never marked and don't pay is left alone.
     */
    suspend fun saveTo(
        expenseId: String,
        state: SplitBillUiState,
    ) {
        if (!state.showsPaidWith && state.paidWith.openedMethodId == null) return
        useCases.markBillPayingMethod(expenseId, state.paidWithToSave)
    }

    /*
     * Why: what the form opens on is set once, from the first list that has methods (a restored account's come a moment
     * later); later the list only follows the methods added or deleted, and a picked method deleted meanwhile goes back
     * to the bill's own mark.
     */
    private fun show(
        methods: List<PayingMethod>,
        mark: PayingMethod?,
    ) {
        val default = methods.firstOrNull { it.isDefault }?.id.takeIf { route.expenseId == null }
        val openedId = mark?.id ?: default
        val added = methods.firstOrNull()?.takeIf { isAwaitingAdded }
        if (added != null) isAwaitingAdded = false
        uiState.update { state ->
            val withMethods = state.paidWith.copy(
                methods = methods,
                isLoaded = true,
            )
            val paidWith = when {
                // Why: the method saved from "Add a payment method" comes back picked, a change of the person's own.
                added != null -> withMethods.copy(methodId = added.id)

                !hasOpened -> withMethods.copy(
                    methodId = openedId,
                    openedMethodId = openedId,
                )

                methods.none { it.id == withMethods.methodId } -> withMethods.copy(
                    methodId = withMethods.openedMethodId,
                )

                else -> withMethods
            }
            state.copy(paidWith = paidWith)
        }
        if (methods.isNotEmpty()) hasOpened = true
    }
}
