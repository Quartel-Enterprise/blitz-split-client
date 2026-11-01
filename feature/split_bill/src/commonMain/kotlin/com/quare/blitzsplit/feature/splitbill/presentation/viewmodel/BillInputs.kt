package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

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
