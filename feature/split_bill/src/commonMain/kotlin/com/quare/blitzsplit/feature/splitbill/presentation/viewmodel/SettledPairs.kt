package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettlements
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/*
 * Why: what saving "Editar conta" as it is typed and deleting the bill would each unsettle (7.4b). One read of the
 * group's bills and payments feeds both; the form comes in through refresh, so showing a pair never feeds back into
 * what works it out, and typing never works the deletion out again.
 */
internal class SettledPairs(
    private val scope: CoroutineScope,
    expenseId: String,
    groupId: String,
    group: Flow<Group?>,
    observeExpenses: ObserveExpenses,
    observeSettlements: ObserveSettlements,
) {
    private val inputs = MutableStateFlow<BillInputs?>(null)

    private val ledger = combine(group, observeExpenses(groupId), observeSettlements(groupId), ::GroupLedger)
        .shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    // Why: what deleting the bill would unsettle, from Room alone, the one source "Apagar esta conta?" says it from.
    val deletionPair: StateFlow<SettledPairChange?> = ledger
        .map { (group, expenses, settlements) ->
            SettledPairChange.findDeletionOrNull(expenseId, group, expenses, settlements)
        }.stateIn(scope, SharingStarted.Eagerly, null)

    // Why: what saving the form would unsettle, null while it can't be saved or the bill isn't read yet.
    private val editPair: Flow<SettledPairChange?> = combine(inputs, ledger) { input, (group, expenses, settlements) ->
        val saved = expenses.firstOrNull { it.id == expenseId }
        if (input == null || group == null || saved == null) return@combine null
        val changed = saved.copy(
            amountMinor = input.amountMinor,
            payers = input.payers,
            shares = input.shares,
            spentAt = input.spentAt,
        )
        SettledPairChange.findOrNull(saved, changed, group, expenses, settlements)
    }

    // Why: after each change to the form, the only way what it would save comes in.
    fun refresh(state: SplitBillUiState) {
        inputs.value = BillInputs.of(state)
    }

    // Why: the save or delete in flight changes Room, which must not reword what the form or the question says.
    fun showIn(uiState: MutableStateFlow<SplitBillUiState>) {
        deletionPair
            .onEach { pair ->
                uiState.update { if (it.isDeleting) it else it.copy(deletion = it.deletion?.copy(settledPair = pair)) }
            }.launchIn(scope)
        editPair
            .onEach { pair -> uiState.update { if (it.isSaving) it else it.copy(settledPair = pair) } }
            .launchIn(scope)
    }
}
