package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface ShareBillUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: each counts only when the sheet takes it: a link there to copy or send, one that can change now.
            is OnVisibilityPicked -> EventAnalytics.Manual("bill_link_visibility_changed")

            OnCopyClicked -> EventAnalytics.Manual("bill_link_copy_clicked")

            OnSendClicked -> EventAnalytics.Manual("bill_link_send_clicked")

            OnDisableClicked -> EventAnalytics.Manual("bill_link_disable_clicked")

            OnRetryClicked -> EventAnalytics.Manual("retry_clicked")

            // Why: a close, which each platform also does its own way: where it went is its screen view.
            OnCloseClicked -> EventAnalytics.NotTracked
        }

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
