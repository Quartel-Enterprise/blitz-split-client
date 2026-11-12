package com.quare.blitzsplit.core.expenses.billmenu

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: a bill's ⋮ (and the row's long press or right click) does the same from the group's Despesas, Início's "Últimas
 * despesas" and "Todas as despesas": Editar, Duplicar and Compartilhar open the bill's screens without opening it first,
 * and Apagar asks Conta's "Apagar esta conta?", kept up to date while a sync changes the bill, and deletes it there.
 * The screen says where each option goes (openOption answers whether it opened: never over something else it opened), sends
 * what it did to Analytics with its source (track) and how a failed delete is said.
 */
class BillMenuController(
    private val useCases: BillMenuUseCases,
    private val scope: CoroutineScope,
    private val openOption: (option: BillMenuOption, groupId: String, expenseId: String) -> Boolean,
    private val track: (step: BillMenuStep) -> Unit,
    private val showDeleteError: suspend () -> Unit,
) {
    val state: StateFlow<BillMenuState>
        field = MutableStateFlow(BillMenuState.idle)

    // Why: the bill the question is about, which a second tap before it draws never replaces.
    private var asked: AskedBill? = null
    private var questionJob: Job? = null

    // Why: Editar, Duplicar or Compartilhar; a bill already gone opens nothing.
    fun open(
        option: BillMenuOption,
        groupId: String,
        expenseId: String,
    ) {
        scope.launch {
            val expense = useCases.observeExpense(expenseId).first() ?: return@launch
            if (option.needsKnownCurrency && !expense.currency.isKnown) return@launch
            if (openOption(option, groupId, expenseId)) track(option.openedStep)
        }
    }

    // Why: "Apagar" asks first, once; the question follows the bill and closes once it is gone, here or elsewhere.
    fun askToDelete(
        groupId: String,
        expenseId: String,
    ) {
        if (asked != null) return
        val bill = AskedBill(groupId, expenseId)
        asked = bill
        var isFirst = true
        questionJob = useCases.observeBillDeletion(groupId, expenseId)
            .onEach { deletion ->
                when {
                    // Why: the delete in flight takes the bill out of Room, which must not close the question shown.
                    state.value.isDeleting -> Unit

                    deletion == null -> close()

                    else -> {
                        if (isFirst) track(BillMenuStep.DELETE_ASKED)
                        isFirst = false
                        state.update { it.copy(billToDelete = deletion) }
                    }
                }
            }.launchIn(scope)
    }

    // Why: the question's "Apagar": the bill leaves the list it was on; a failure closes the question and says so.
    fun confirmDelete() {
        val bill = asked ?: return
        val current = state.value
        if (current.billToDelete == null || current.isDeleting) return
        track(BillMenuStep.DELETE_CONFIRMED)
        state.update { it.copy(isDeleting = true) }
        scope.launch {
            val result = useCases.deleteExpense(bill.expenseId)
            close()
            result.onFailure { showDeleteError() }
        }
    }

    // Why: "Cancelar", or the question closed some other way; never while the bill is being deleted.
    fun dismissDelete() {
        val current = state.value
        if (current.billToDelete == null || current.isDeleting) return
        track(BillMenuStep.DELETE_CANCELLED)
        close()
    }

    private fun close() {
        questionJob?.cancel()
        questionJob = null
        asked = null
        state.value = BillMenuState.idle
    }

    private data class AskedBill(
        val groupId: String,
        val expenseId: String,
    )
}
