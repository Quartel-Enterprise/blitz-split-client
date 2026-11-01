package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface GroupsUiEvent : UiEvent {
    data object OnCreateGroupClicked : GroupsUiEvent

    data object OnJoinWithLinkClicked : GroupsUiEvent

    data class OnGroupClicked(
        val groupId: String,
    ) : GroupsUiEvent

    data object OnProfileClicked : GroupsUiEvent

    data object OnBalancesTabClicked : GroupsUiEvent

    data object OnContactsTabClicked : GroupsUiEvent

    data object OnActivityTabClicked : GroupsUiEvent

    data object OnRetrySyncClicked : GroupsUiEvent

    // Why: pulled down on Android and iOS.
    data object OnRefresh : GroupsUiEvent

    // Why: "Rachar conta", the FAB on Android, + on iOS, the main button on the web and the Mac.
    data object OnSplitBillClicked : GroupsUiEvent

    // Why: the "A receber" and "A pagar" buttons of "Saldo geral".
    data object OnToReceiveClicked : GroupsUiEvent

    data object OnToPayClicked : GroupsUiEvent

    // Why: "Quitados" expanded or collapsed on a phone, the Quitados or Em aberto tab on the web and the Mac.
    data class OnShowSettledChanged(
        val isShown: Boolean,
    ) : GroupsUiEvent

    data class OnFilterChanged(
        val query: String,
    ) : GroupsUiEvent

    /*
     * Why: the row's ⋮ menu (⋯ on Apple and the web) or its long press: "Convidar pessoas", "Sair do grupo" and
     * "Apagar grupo" of that group, without opening it.
     */
    data class OnInviteClicked(
        val groupId: String,
    ) : GroupsUiEvent

    data class OnLeaveGroupClicked(
        val groupId: String,
    ) : GroupsUiEvent

    data class OnDeleteGroupClicked(
        val groupId: String,
    ) : GroupsUiEvent

    // Why: the main button of the dialog they open: "Sair", "Apagar para todos", "Acertar" or "Ver saldos".
    data object OnGroupExitConfirmed : GroupsUiEvent

    // Why: "Cancelar" or "Agora não", or the dialog dismissed.
    data object OnGroupExitDismissed : GroupsUiEvent

    // Why: the snackbar of a deleted group went away, so the next one, if any, can show.
    data object OnDeletedGroupNoticeShown : GroupsUiEvent

    // Why: "Desfazer" on the notice of a payment just recorded from a row's "Acertar".
    data object OnUndoPaymentClicked : GroupsUiEvent

    // Why: the notice went away without Desfazer.
    data object OnRecordedPaymentNoticeShown : GroupsUiEvent
}
