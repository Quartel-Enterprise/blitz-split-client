package com.quare.blitzsplit.feature.members.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface MembersUiEvent : UiEvent {
    data object OnBackClicked : MembersUiEvent

    // Why: "Compartilhar" on the link card opens "Convidar", with the QR code.
    data object OnShareClicked : MembersUiEvent

    // Why: "Despesas" and "Saldos", the tabs next to "Membros" on Android.
    data object OnOtherTabClicked : MembersUiEvent

    data object OnAddClicked : MembersUiEvent

    data class OnRenameClicked(
        val memberId: String,
    ) : MembersUiEvent

    data class OnPersonalInviteClicked(
        val memberId: String,
    ) : MembersUiEvent

    // Why: "Remover do grupo" in the member's menu, which asks first.
    data class OnRemoveClicked(
        val memberId: String,
    ) : MembersUiEvent

    data object OnRemoveConfirmed : MembersUiEvent

    data object OnRemoveDismissed : MembersUiEvent

    data class OnDraftChanged(
        val draft: String,
    ) : MembersUiEvent

    data class OnSuggestionClicked(
        val name: String,
    ) : MembersUiEvent

    data object OnSaveClicked : MembersUiEvent

    data object OnEditorDismissed : MembersUiEvent
}
