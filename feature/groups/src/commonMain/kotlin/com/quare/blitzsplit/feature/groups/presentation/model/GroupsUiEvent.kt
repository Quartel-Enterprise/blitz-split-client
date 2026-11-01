package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface GroupsUiEvent : UiEvent {
    data object OnCreateGroupClicked : GroupsUiEvent

    data object OnJoinWithLinkClicked : GroupsUiEvent

    data class OnGroupClicked(
        val groupId: String,
    ) : GroupsUiEvent

    data object OnProfileClicked : GroupsUiEvent

    /*
     * Why: the UI draws, or stops drawing, the group beside the list, as its window decides (a tablet lying down, an
     * unfolded book, an iPad lying down). Then a tap on the list replaces the group beside it, and the search opens
     * its first result there.
     */
    data class OnDetailPaneChanged(
        val isShown: Boolean,
    ) : GroupsUiEvent

    data object OnHomeTabClicked : GroupsUiEvent

    data object OnBalancesTabClicked : GroupsUiEvent

    // Why: the tab bar of iOS and the Mac's sidebar stay over what the groups opened, Saldos included.
    data object OnGroupsTabClicked : GroupsUiEvent

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

    // Why: "Lista" or "Grade" in the view menu (Android and iOS), the toggle (web) or the toolbar's segments (Mac).
    data class OnDisplayChanged(
        val display: GroupsDisplay,
    ) : GroupsUiEvent

    // Why: "Compacto", "Padrão" or "Grande": the menu, the web's Select, the Mac's slider or its View menu.
    data class OnGridSizeChanged(
        val size: GroupGridSize,
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

    // Why: Android's magnifier, iOS's search tab, the web's "Buscar…" or ⌘K, the Mac's toolbar field.
    data object OnSearchOpened : GroupsUiEvent

    // Why: back or ✕ on Android, Esc or a click outside on the web, the field cancelled on the Mac.
    data object OnSearchClosed : GroupsUiEvent

    // Why: iOS's search tab shows what its results opened, so leaving it for Groups also goes back to the groups.
    data object OnSearchTabLeft : GroupsUiEvent

    data class OnSearchQueryChanged(
        val query: String,
    ) : GroupsUiEvent

    // Why: "Tudo", "Grupos", "Pessoas" or "Despesas".
    data class OnSearchScopeChanged(
        val scope: SearchScope,
    ) : GroupsUiEvent

    // Why: a person found opens the members of the group they were found in; a group found is OnGroupClicked.
    data class OnSearchPersonClicked(
        val groupId: String,
    ) : GroupsUiEvent

    // Why: a bill found opens over its group, so back goes to the group's bills, as from Despesas.
    data class OnSearchBillClicked(
        val groupId: String,
        val expenseId: String,
    ) : GroupsUiEvent
}
