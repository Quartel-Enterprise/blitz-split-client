package com.quare.blitzsplit.feature.watchbalances.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface WatchBalancesUiEvent : UiEvent {
    // Why: a group's row (15.3), to its pairs and "I paid" (15.4).
    data class OnGroupClicked(
        val groupId: String,
        val currency: GroupCurrency,
    ) : WatchBalancesUiEvent {
        override val analytics: EventAnalytics
            get() = EventAnalytics.Automatic("watch_group_opened")
    }

    // Why: "Sign out" at the end of Balances, the one way to disconnect only the watch.
    data object OnSignOutClicked : WatchBalancesUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_sign_out_clicked")
    }

    // Why: ✓ on "Sign out of this watch?".
    data object OnSignOutConfirmed : WatchBalancesUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_sign_out_confirmed")
    }

    // Why: ✕ on the question, or swiped away.
    data object OnSignOutDismissed : WatchBalancesUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_sign_out_cancelled")
    }

    // Why: Cancel on "Sign out scheduled": the watch stays signed in.
    data object OnCancelScheduledSignOutClicked : WatchBalancesUiEvent {
        override val analytics: EventAnalytics = EventAnalytics.Automatic("watch_sign_out_scheduled_cancelled")
    }
}
