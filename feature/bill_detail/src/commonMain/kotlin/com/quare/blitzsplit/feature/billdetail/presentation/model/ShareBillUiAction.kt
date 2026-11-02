package com.quare.blitzsplit.feature.billdetail.presentation.model

sealed interface ShareBillUiAction {
    data class CopyLink(
        val link: String,
    ) : ShareBillUiAction

    // Why: each platform writes the message around the link, with the bill's name.
    data class SendLink(
        val link: String,
        val description: String,
    ) : ShareBillUiAction

    // Why: "Não deu para mudar o link. Tente de novo." or "Não deu para desativar o link. Tente de novo."
    data class ShowError(
        val error: ShareBillError,
    ) : ShareBillUiAction
}
