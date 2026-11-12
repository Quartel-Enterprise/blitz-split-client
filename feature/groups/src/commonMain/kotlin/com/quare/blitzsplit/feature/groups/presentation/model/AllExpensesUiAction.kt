package com.quare.blitzsplit.feature.groups.presentation.model

sealed interface AllExpensesUiAction {
    // Why: a bill deleted from its ⋮ couldn't be, which is rare enough to just say so; it is still there.
    data object ShowBillDeleteError : AllExpensesUiAction
}
