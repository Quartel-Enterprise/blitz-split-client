package com.quare.blitzsplit.core.expenses.billmenu

import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteExpense
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveBillDeletion
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpense

// Why: what every screen with a bill's ⋮ hands its BillMenuController, so each ViewModel takes it whole.
class BillMenuUseCases(
    internal val observeExpense: ObserveExpense,
    internal val observeBillDeletion: ObserveBillDeletion,
    internal val deleteExpense: DeleteExpense,
)
