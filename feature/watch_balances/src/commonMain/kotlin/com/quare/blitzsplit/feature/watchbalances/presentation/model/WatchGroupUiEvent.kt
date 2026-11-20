package com.quare.blitzsplit.feature.watchbalances.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface WatchGroupUiEvent : UiEvent {
    // Why: "I paid" (15.4), the EdgeButton or the row of the person you paid.
    data class OnIPaidClicked(
        val memberId: String,
    ) : WatchGroupUiEvent {
        override val analytics: EventAnalytics
            get() = EventAnalytics.Automatic("watch_i_paid_clicked")
    }

    // Why: ✓ on "Paid R$ 98.00 to Carla?" (15.5).
    data object OnPaymentConfirmed : WatchGroupUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Manual("watch_payment_recorded")
    }

    // Why: ✕ on the question, or swiped away.
    data object OnConfirmDismissed : WatchGroupUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_i_paid_cancelled")
    }

    // Why: "Payment recorded" (15.6) finished showing, so the watch goes back to the balances.
    data object OnRecordedShown : WatchGroupUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.NotTracked
    }
}
