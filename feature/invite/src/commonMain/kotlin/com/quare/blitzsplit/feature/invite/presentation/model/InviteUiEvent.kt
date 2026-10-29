package com.quare.blitzsplit.feature.invite.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface InviteUiEvent : UiEvent {
    data object OnCloseClicked : InviteUiEvent

    data object OnCopyClicked : InviteUiEvent

    // Why: "Compartilhar link", the system's share sheet.
    data object OnShareClicked : InviteUiEvent

    // Why: "Ver quem já entrou", the group's "Membros".
    data object OnSeeWhoJoinedClicked : InviteUiEvent

    // Why: "Tentar de novo", while the link failed to come.
    data object OnRetryClicked : InviteUiEvent

    // Why: "Ir para o grupo", offered while the link is pending.
    data object OnGoToGroupClicked : InviteUiEvent
}
