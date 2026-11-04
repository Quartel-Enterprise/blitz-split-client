package com.quare.blitzsplit.feature.join.presentation.model

import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

sealed interface JoinPickUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            is OnMemberSelected -> EventAnalytics.Automatic("join_pick_member_selected")

            OnJoinClicked -> EventAnalytics.Automatic("join_pick_join_clicked")

            OnNotInListClicked -> EventAnalytics.Automatic("join_pick_not_in_list_clicked")

            is OnNewNameChanged -> EventAnalytics.NotTracked

            OnNewMemberConfirmed -> EventAnalytics.Automatic("join_pick_new_member_confirmed")

            OnNewMemberDismissed -> EventAnalytics.NotTracked
        }

    data object OnBackClicked : JoinPickUiEvent

    data class OnMemberSelected(
        val memberId: String,
    ) : JoinPickUiEvent

    // Why: "Entrar como Bruno".
    data object OnJoinClicked : JoinPickUiEvent

    // Why: "Não estou na lista", which opens "Entrar com outro nome".
    data object OnNotInListClicked : JoinPickUiEvent

    data class OnNewNameChanged(
        val name: String,
    ) : JoinPickUiEvent

    // Why: "Entrar no grupo" in the sheet.
    data object OnNewMemberConfirmed : JoinPickUiEvent

    data object OnNewMemberDismissed : JoinPickUiEvent
}
