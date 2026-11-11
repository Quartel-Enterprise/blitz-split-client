package com.quare.blitzsplit.feature.legal.presentation.model

import com.quare.blitzsplit.core.legal.domain.model.LegalLink
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface LegalDocumentUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: the screen view under it coming back says it.
            OnBackClicked -> EventAnalytics.NotTracked

            OnRetryClicked -> EventAnalytics.Automatic("legal_document_retry_clicked")

            // Why: which kind of link, never its address, sent by the ViewModel.
            is OnLinkClicked -> EventAnalytics.Manual("legal_document_link_clicked")
        }

    // Why: the bar's back, the Mac sheet's Done, Esc or the system's back.
    data object OnBackClicked : LegalDocumentUiEvent

    // Why: "Try again" where the text couldn't be downloaded.
    data object OnRetryClicked : LegalDocumentUiEvent

    // Why: a link inside the text.
    data class OnLinkClicked(
        val link: LegalLink,
    ) : LegalDocumentUiEvent
}
