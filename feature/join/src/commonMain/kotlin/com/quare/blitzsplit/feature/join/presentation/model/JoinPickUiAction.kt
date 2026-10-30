package com.quare.blitzsplit.feature.join.presentation.model

sealed interface JoinPickUiAction {
    // Why: the server couldn't be reached; nothing changed, so trying again is safe.
    data object ShowJoinError : JoinPickUiAction
}
