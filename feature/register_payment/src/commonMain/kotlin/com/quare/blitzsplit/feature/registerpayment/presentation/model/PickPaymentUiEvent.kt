package com.quare.blitzsplit.feature.registerpayment.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface PickPaymentUiEvent : UiEvent {
    // Why: a pair, which opens Registrar pagamento for it in this sheet's place.
    data class OnPairClicked(
        val groupId: String,
        val fromMemberId: String,
        val toMemberId: String,
    ) : PickPaymentUiEvent

    // Why: the sheet's ✕, a swipe down, Esc or the scrim.
    data object OnDismissed : PickPaymentUiEvent
}
