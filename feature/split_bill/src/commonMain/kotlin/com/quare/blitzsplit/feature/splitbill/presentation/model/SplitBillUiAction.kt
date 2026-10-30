package com.quare.blitzsplit.feature.splitbill.presentation.model

sealed interface SplitBillUiAction {
    // Why: the device couldn't save the bill, which is rare enough to just say so.
    data object ShowSaveError : SplitBillUiAction
}
