package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface ShareBillUiEvent : UiEvent {
    // Why: "Só quem está na conta" or "Qualquer pessoa com o link".
    data class OnVisibilityPicked(
        val visibility: BillLinkVisibility,
    ) : ShareBillUiEvent

    data object OnCopyClicked : ShareBillUiEvent

    // Why: "Enviar link", the system's share.
    data object OnSendClicked : ShareBillUiEvent

    data object OnDisableClicked : ShareBillUiEvent

    data object OnRetryClicked : ShareBillUiEvent

    data object OnCloseClicked : ShareBillUiEvent
}
