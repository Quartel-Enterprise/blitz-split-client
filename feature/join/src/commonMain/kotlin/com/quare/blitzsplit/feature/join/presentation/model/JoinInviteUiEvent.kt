package com.quare.blitzsplit.feature.join.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "join_invite"

sealed interface JoinInviteUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            OnJoinClicked -> EventAnalytics.Automatic("join_invite_join_clicked")

            OnNotMeClicked -> EventAnalytics.Automatic("join_invite_not_me_clicked")

            OnNotNowClicked -> EventAnalytics.Automatic("join_invite_not_now_clicked")

            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            OnRetryClicked -> EventAnalytics.Automatic("retry_clicked", mapOf("source" to SOURCE))

            OnGoHomeClicked -> EventAnalytics.Automatic("join_invite_go_home_clicked")

            OnScanAnotherClicked -> EventAnalytics.Automatic("join_invite_scan_another_clicked")
        }

    /*
     * Why: "Entrar no grupo", which goes on to "Quem é você no grupo?"; on a personal invite "Entrar como Carla", which
     * joins straight in as that member.
     */
    data object OnJoinClicked : JoinInviteUiEvent

    // Why: "Não sou Carla" on a personal invite, which goes on to the whole list and leaves the link unused.
    data object OnNotMeClicked : JoinInviteUiEvent

    // Why: "Agora não" and the close button.
    data object OnNotNowClicked : JoinInviteUiEvent

    // Why: Android's system back, which leaves as "Agora não" does but is no tap of it.
    data object OnBackClicked : JoinInviteUiEvent

    // Why: "Tentar de novo", offline.
    data object OnRetryClicked : JoinInviteUiEvent

    // Why: "Ir para o início", for an invite that isn't valid or can't open.
    data object OnGoHomeClicked : JoinInviteUiEvent

    // Why: "Escanear outro QR", for an invite that isn't valid: the reader takes its place.
    data object OnScanAnotherClicked : JoinInviteUiEvent
}
