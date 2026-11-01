package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveSettlements
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/*
 * Why: what saving the form as it is typed and deleting the bill would each unsettle (7.4b), the first null while
 * the form can't be saved. Only who paid and the parts move a balance, so typing the description reads nothing.
 */
internal fun observeSettledPairs(
    expenseId: String,
    groupId: String,
    uiState: Flow<SplitBillUiState>,
    observeGroup: ObserveGroup,
    observeExpenses: ObserveExpenses,
    observeSettlements: ObserveSettlements,
): Flow<Pair<SettledPairChange?, SettledPairChange?>> = combine(
    uiState.map(::billChangeOf).distinctUntilChanged(),
    observeGroup(groupId),
    observeExpenses(groupId),
    observeSettlements(groupId),
) { change, group, expenses, settlements ->
    val saved = expenses.firstOrNull { it.id == expenseId }
    if (group == null || saved == null) return@combine null to null
    val changed = change?.let { (payers, shares) ->
        saved.copy(
            amountMinor = shares.sumOf { it.amountMinor },
            payers = payers,
            shares = shares,
        )
    }
    val editPair = changed?.let { SettledPairChange.findOrNull(saved, it, group, expenses, settlements) }
    editPair to SettledPairChange.findDeletionOrNull(expenseId, group, expenses, settlements)
}

private fun billChangeOf(state: SplitBillUiState): Pair<List<ExpensePayer>, List<ExpenseShare>>? {
    val shares = state.split.shares
    return if (state.isLoaded && state.isAmountValid && state.isPayerValid && shares != null) {
        state.payersToSave to shares
    } else {
        null
    }
}
