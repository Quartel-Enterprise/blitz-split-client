package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SettledPairChange
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// Why: what a bill's settled pairs are worked out from, as Room has the group now (7.4b).
internal data class GroupLedger(
    val group: Group?,
    val expenses: List<Expense>,
    val settlements: List<Settlement>,
)

/*
 * Why: the form as saving it would write the bill, null while it can't be saved. Only who paid, the parts, the total
 * and "Quando" move a balance or a payment's place after the bill, so typing the description reads nothing.
 */
internal data class BillInputs(
    val amountMinor: Long,
    val payers: List<ExpensePayer>,
    val shares: List<ExpenseShare>,
    val spentAt: Long,
) {
    companion object {
        fun of(state: SplitBillUiState): BillInputs? {
            val amountMinor = state.amountMinor
            val shares = state.split.shares
            if (!state.isLoaded || !state.isAmountValid || !state.isPayerValid) return null
            if (amountMinor == null || shares == null) return null
            return BillInputs(
                amountMinor = amountMinor,
                payers = state.payersToSave,
                shares = shares,
                spentAt = state.spentAt,
            )
        }
    }
}

// Why: what saving the form as it is typed would unsettle, null while it can't be saved or the bill isn't read yet.
internal fun observeEditSettledPair(
    expenseId: String,
    inputs: Flow<BillInputs?>,
    ledger: Flow<GroupLedger>,
): Flow<SettledPairChange?> = combine(inputs, ledger) { input, (group, expenses, settlements) ->
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

// Why: what deleting the bill would unsettle, from Room alone, so typing in the form never works it out again.
internal fun observeDeletionSettledPair(
    expenseId: String,
    ledger: Flow<GroupLedger>,
): Flow<SettledPairChange?> = ledger.map { (group, expenses, settlements) ->
    SettledPairChange.findDeletionOrNull(expenseId, group, expenses, settlements)
}
