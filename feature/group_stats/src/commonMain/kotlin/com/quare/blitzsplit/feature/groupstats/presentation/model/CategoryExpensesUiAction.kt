package com.quare.blitzsplit.feature.groupstats.presentation.model

sealed interface CategoryExpensesUiAction {
    // Why: a bill deleted from its ⋮ couldn't be, which is rare enough to just say so; it is still there.
    data object ShowBillDeleteError : CategoryExpensesUiAction
}
