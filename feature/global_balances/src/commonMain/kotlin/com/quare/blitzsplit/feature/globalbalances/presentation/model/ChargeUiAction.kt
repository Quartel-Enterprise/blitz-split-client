package com.quare.blitzsplit.feature.globalbalances.presentation.model

sealed interface ChargeUiAction {
    // Why: the system share sheet (WhatsApp, SMS, e-mail); the browser copies the message when it has none.
    data class ShareMessage(
        val message: String,
    ) : ChargeUiAction

    // Why: "Copiado".
    data class CopyMessage(
        val message: String,
    ) : ChargeUiAction
}
