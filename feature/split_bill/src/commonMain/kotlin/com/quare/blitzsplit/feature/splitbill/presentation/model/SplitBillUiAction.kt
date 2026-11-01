package com.quare.blitzsplit.feature.splitbill.presentation.model

sealed interface SplitBillUiAction {
    // Why: the device couldn't save the bill, which is rare enough to just say so.
    data object ShowSaveError : SplitBillUiAction

    // Why: the device couldn't delete the bill, as rare.
    data object ShowDeleteError : SplitBillUiAction

    // Why: the device couldn't save the category (7.8), as rare.
    data object ShowCategorySaveError : SplitBillUiAction

    data object ShowCategoryDeleteError : SplitBillUiAction
}
