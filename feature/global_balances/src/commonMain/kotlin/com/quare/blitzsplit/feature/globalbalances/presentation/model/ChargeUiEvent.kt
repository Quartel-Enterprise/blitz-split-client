package com.quare.blitzsplit.feature.globalbalances.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface ChargeUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a toggle, sent only while the switch takes it, since it locks once the message was edited.
            is OnPixKeyToggled -> EventAnalytics.Manual("charge_pix_key_toggled")

            // Why: typed letter by letter; whether it was edited shows when the message goes.
            is OnMessageChanged -> EventAnalytics.NotTracked

            // Why: only while the message is drawn, never a late tap once it closed.
            is OnShareClicked -> EventAnalytics.Manual("charge_share_clicked")

            is OnCopyClicked -> EventAnalytics.Manual("charge_copy_clicked")

            // Why: a close, which each platform also does its own way: where it went is its screen view.
            OnDismissed -> EventAnalytics.NotTracked
        }

    // Why: "Incluir minha chave Pix".
    data class OnPixKeyToggled(
        val isIncluded: Boolean,
    ) : ChargeUiEvent

    // Why: the message as typed; the UIs send it once something was typed.
    data class OnMessageChanged(
        val message: String,
    ) : ChargeUiEvent

    // Why: "Enviar", with the message as the UI shows it, written in its own words.
    data class OnShareClicked(
        val message: String,
    ) : ChargeUiEvent

    // Why: "Copiar mensagem".
    data class OnCopyClicked(
        val message: String,
    ) : ChargeUiEvent

    /*
     * Why: ✕ or Esc. A swipe down, the scrim and the back gesture pop the route through the platform instead; an
     * Android dialog keeps an edited message on a stray tap outside, as every dialog route there does.
     */
    data object OnDismissed : ChargeUiEvent
}
