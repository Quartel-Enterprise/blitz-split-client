package com.quare.blitzsplit.feature.join.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface JoinInviteUiEvent : UiEvent {
    // Why: "Entrar no grupo", which goes on to "Quem é você no grupo?".
    data object OnJoinClicked : JoinInviteUiEvent

    // Why: "Agora não" and the close button.
    data object OnNotNowClicked : JoinInviteUiEvent

    // Why: "Tentar de novo", offline.
    data object OnRetryClicked : JoinInviteUiEvent

    // Why: "Ir para o início", for an invite that isn't valid or can't open.
    data object OnGoHomeClicked : JoinInviteUiEvent
}
