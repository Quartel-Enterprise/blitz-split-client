package com.quare.blitzsplit.feature.members.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface MembersUiEvent : UiEvent {
    data object OnBackClicked : MembersUiEvent

    // Why: "Compartilhar" on the link card opens "Convidar", with the QR code.
    data object OnShareClicked : MembersUiEvent

    // Why: the "Despesas" tab, the group's own screen.
    data object OnExpensesTabClicked : MembersUiEvent

    // Why: the "Saldos" tab.
    data object OnBalancesTabClicked : MembersUiEvent

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

    data class OnDraftChanged(
        val draft: String,
    ) : MembersUiEvent

    data class OnSuggestionClicked(
        val name: String,
    ) : MembersUiEvent

    data object OnSaveClicked : MembersUiEvent

    data object OnEditorDismissed : MembersUiEvent

    // Why: "Proteja seu acesso", which opens the profile, where the code is kept and Google or Apple connected.
    data object OnProtectAccessClicked : MembersUiEvent
}
