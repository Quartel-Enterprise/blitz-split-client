package com.quare.blitzsplit.feature.registerpayment.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface PickPaymentUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            is OnPairClicked -> EventAnalytics.Automatic("pick_payment_pair_selected")

            // Why: a close, which each platform also does its own way: where it went is its screen view.
            OnDismissed -> EventAnalytics.NotTracked
        }

    // Why: a pair, which opens Registrar pagamento for it in this sheet's place.
    data class OnPairClicked(
        val groupId: String,
        val fromMemberId: String,
        val toMemberId: String,
    ) : PickPaymentUiEvent

    // Why: the sheet's ✕, a swipe down, Esc or the scrim.
    data object OnDismissed : PickPaymentUiEvent
}
