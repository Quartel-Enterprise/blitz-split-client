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

/*
 * Why: "Paid with" (private payment methods): a new bill opens on the person's default method, a duplicate on its
 * original's mark, a bill being edited on its own; what they pick is saved for them alone once the bill is.
 */
internal class PaidWithPicking(
    private val uiState: MutableStateFlow<SplitBillUiState>,
    private val route: Route.SplitBill,
    private val useCases: BillPayingMethodUseCases,
    private val trackEvent: TrackEvent,
    scope: CoroutineScope,
) {
    private var hasOpened = false

    init {
        val markedId = route.expenseId ?: route.duplicatesExpenseId
        flow { emit(markedId?.let { useCases.observeBillPayingMethod(it).first() }) }
            .flatMapLatest { mark -> useCases.observePayingMethods().map { methods -> methods to mark } }
            .onEach { (methods, mark) -> show(methods, mark) }
            .launchIn(scope)
    }

    fun pick(methodId: String?) {
        val state = uiState.value
        if (!state.showsPaidWith || methodId == state.paidWith.methodId) return
        if (methodId != null && state.paidWith.methods.none { it.id == methodId }) return
        trackEvent("split_bill_paid_with_picked", mapOf("has_method" to (methodId != null)))
        uiState.update { it.copy(paidWith = it.paidWith.copy(methodId = methodId)) }
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
        uiState.update { state ->
            val withMethods = state.paidWith.copy(methods = methods)
            val paidWith = when {
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
