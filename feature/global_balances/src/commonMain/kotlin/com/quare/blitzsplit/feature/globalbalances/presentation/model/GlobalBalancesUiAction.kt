package com.quare.blitzsplit.feature.globalbalances.presentation.model

sealed interface GlobalBalancesUiAction {
    // Why: the system share sheet (WhatsApp, SMS, e-mail); the browser copies the message when it has none.
    data class ShareMessage(
        val message: String,
    ) : GlobalBalancesUiAction

    // Why: "Copiado".
    data class CopyMessage(
        val message: String,
    ) : GlobalBalancesUiAction

    // Why: the tabs Contatos and Atividade are not built yet.
    data object ShowComingSoon : GlobalBalancesUiAction

    // Why: the device couldn't undo the payments "Desfazer" asked for, which is rare enough to just say so.
    data object ShowChangeError : GlobalBalancesUiAction
}
