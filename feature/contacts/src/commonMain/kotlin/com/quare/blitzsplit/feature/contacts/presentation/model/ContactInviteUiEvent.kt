package com.quare.blitzsplit.feature.contacts.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface ContactInviteUiEvent : UiEvent {
    data class OnGroupPicked(
        val groupId: String,
    ) : ContactInviteUiEvent

    // Why: the "Novo grupo" chip, which opens Novo grupo in the invite's place.
    data object OnNewGroupClicked : ContactInviteUiEvent

    data object OnCopyClicked : ContactInviteUiEvent

    data object OnShareClicked : ContactInviteUiEvent

    data object OnCloseClicked : ContactInviteUiEvent
}
