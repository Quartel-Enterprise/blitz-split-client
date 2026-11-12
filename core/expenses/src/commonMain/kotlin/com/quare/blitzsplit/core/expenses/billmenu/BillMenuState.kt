package com.quare.blitzsplit.core.expenses.billmenu

import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion

/*
 * Why: where a bill's ⋮ "Apagar" stands, the same on the group's Despesas, Início and "Todas as despesas". Not a
 * screen's UiState: BillMenuController produces it and each screen copies it into its own.
 */
data class BillMenuState(
    // Why: "Apagar esta conta?" is open while not null, worded from the bill as it is now.
    val billToDelete: BillDeletion?,
    // Why: its "Apagar" waits while the device deletes the bill.
    val isDeleting: Boolean,
) {
    companion object {
        val idle = BillMenuState(
            billToDelete = null,
            isDeleting = false,
        )
    }
}
