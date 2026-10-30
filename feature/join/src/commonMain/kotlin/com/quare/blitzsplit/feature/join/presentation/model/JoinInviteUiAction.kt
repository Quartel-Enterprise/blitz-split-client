package com.quare.blitzsplit.feature.join.presentation.model

sealed interface JoinInviteUiAction {
    // Why: "Entrar como Carla" on a personal invite couldn't reach the server; nothing changed, so trying again is safe.
    data object ShowJoinError : JoinInviteUiAction
}
