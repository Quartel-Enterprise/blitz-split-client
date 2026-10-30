package com.quare.blitzsplit.feature.join.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface JoinPickUiEvent : UiEvent {
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
