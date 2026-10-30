package com.quare.blitzsplit.feature.billdetail.presentation.model

sealed interface BillDetailUiAction {
    // Why: the device couldn't delete the bill, which is rare enough to just say so.
    data object ShowDeleteError : BillDetailUiAction
}
