package com.quare.blitzsplit.feature.registerpayment.presentation.model

sealed interface RegisterPaymentUiAction {
    // Why: the device couldn't save the payment, which is rare enough to just say so.
    data object ShowSaveError : RegisterPaymentUiAction
}
