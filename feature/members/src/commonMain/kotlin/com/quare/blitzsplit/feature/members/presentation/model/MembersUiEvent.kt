package com.quare.blitzsplit.feature.members.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface MembersUiEvent : UiEvent {
    // Why: "Compartilhar" on the link card opens "Convidar", with the QR code.
    data object OnShareClicked : MembersUiEvent

    data object OnAddClicked : MembersUiEvent

    data class OnRenameClicked(
        val memberId: String,
    ) : MembersUiEvent

    // Why: "Convite pessoal para Carla" in the member's menu, which opens the sheet with the member's link.
    data class OnPersonalInviteClicked(
        val memberId: String,
    ) : MembersUiEvent

    data object OnPersonalInviteDismissed : MembersUiEvent

    // Why: "Copiar" on the personal invite.
    data object OnPersonalLinkCopyClicked : MembersUiEvent

    // Why: "Compartilhar com Carla", the system's share sheet.
    data object OnPersonalLinkShareClicked : MembersUiEvent

    // Why: "Remover do grupo" in the member's menu, which asks first.
    data class OnRemoveClicked(
        val memberId: String,
    ) : MembersUiEvent

    data object OnRemoveConfirmed : MembersUiEvent

    data object OnRemoveDismissed : MembersUiEvent

    // Why: "Liberar nome" in the menu of someone who joined, for the group's creator, which asks first.
    data class OnReleaseClicked(
        val memberId: String,
    ) : MembersUiEvent

    data object OnReleaseConfirmed : MembersUiEvent

    data object OnReleaseDismissed : MembersUiEvent

    data class OnDraftChanged(
        val draft: String,
    ) : MembersUiEvent

    data class OnSuggestionClicked(
        val name: String,
    ) : MembersUiEvent

    data object OnSaveClicked : MembersUiEvent

    data object OnEditorDismissed : MembersUiEvent
}
