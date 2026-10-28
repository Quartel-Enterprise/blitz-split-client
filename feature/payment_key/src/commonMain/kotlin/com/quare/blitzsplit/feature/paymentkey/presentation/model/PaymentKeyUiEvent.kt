package com.quare.blitzsplit.feature.paymentkey.presentation.model

import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface PaymentKeyUiEvent : UiEvent {
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
