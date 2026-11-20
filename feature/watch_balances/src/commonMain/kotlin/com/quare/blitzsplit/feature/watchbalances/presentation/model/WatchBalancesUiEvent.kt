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
}
