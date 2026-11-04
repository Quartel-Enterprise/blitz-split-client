package com.quare.blitzsplit.feature.paymentkey.presentation.model

import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

sealed interface PaymentKeyUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            is OnTypeSelected -> EventAnalytics.Automatic("payment_key_type_selected", mapOf("type" to type))

            // Why: typed letter by letter, and the field losing focus, which nobody taps; the key shows when saved.
            is OnInputChanged,
            OnInputFocusLost,
            -> EventAnalytics.NotTracked

            is OnPasted -> EventAnalytics.Automatic("payment_key_paste_clicked")

            // Why: a toggle, sent with where it ended up, only while the switch takes it (not the only key of its method).
            is OnDefaultChanged -> EventAnalytics.Manual("payment_key_default_toggled")

            // Why: the tap, then the key once it is saved.
            OnSaveClicked -> EventAnalytics.Manual(setOf("payment_key_save_clicked", "payment_key_saved"))

            OnDeleteClicked -> EventAnalytics.Automatic("payment_key_delete_clicked")

            // Why: only an answer to a question still open, since Apple's alert also closes itself after its button.
            OnDeleteConfirmed -> EventAnalytics.Manual("payment_key_delete_confirmed")

            OnDeleteDismissed -> EventAnalytics.Manual("payment_key_delete_cancelled")
        }

    data object OnBackClicked : PaymentKeyUiEvent

    data class OnTypeSelected(
        val type: PaymentKeyType,
    ) : PaymentKeyUiEvent

    data class OnInputChanged(
        val input: String,
    ) : PaymentKeyUiEvent

    // Why: "Colar" replaces the field with what each platform read from its clipboard.
    data class OnPasted(
        val text: String,
    ) : PaymentKeyUiEvent

    data object OnInputFocusLost : PaymentKeyUiEvent

    data class OnDefaultChanged(
        val isDefault: Boolean,
    ) : PaymentKeyUiEvent

    data object OnSaveClicked : PaymentKeyUiEvent

    data object OnDeleteClicked : PaymentKeyUiEvent

    data object OnDeleteConfirmed : PaymentKeyUiEvent

    data object OnDeleteDismissed : PaymentKeyUiEvent
}
