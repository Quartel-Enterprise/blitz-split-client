package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "groups"

sealed interface GroupsUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            OnCreateGroupClicked -> EventAnalytics.Automatic("create_group_clicked", mapOf("source" to SOURCE))

            OnJoinWithLinkClicked -> EventAnalytics.Automatic("join_with_link_clicked", mapOf("source" to SOURCE))

            is OnGroupClicked -> EventAnalytics.Automatic("group_clicked", mapOf("source" to SOURCE))

            OnProfileClicked -> EventAnalytics.Automatic("profile_avatar_clicked", mapOf("source" to SOURCE))

            // Why: the web's logo, which opens Home; the tabs it stands beside are not tracked, their screen view is.
            OnBrandClicked -> EventAnalytics.Automatic("brand_clicked", mapOf("source" to SOURCE))

            // Why: the window drawing the group beside the list or not, never a tap.
            is OnDetailPaneChanged -> EventAnalytics.NotTracked

            /*
             * Why: a tab of the home's bar, rail or sidebar, which each platform routes through whichever screen is in front:
             * where it went is its screen view.
             */
            OnHomeTabClicked,
            OnBalancesTabClicked,
            OnGroupsTabClicked,
            OnContactsTabClicked,
            -> EventAnalytics.NotTracked

            OnRetrySyncClicked -> EventAnalytics.Automatic("sync_retry_clicked", mapOf("source" to SOURCE))

            OnRefresh -> EventAnalytics.Automatic("pull_to_refresh", mapOf("source" to SOURCE))

            OnSplitBillClicked -> EventAnalytics.Automatic("split_bill_clicked", mapOf("source" to SOURCE))

            OnOverallBalanceClicked -> EventAnalytics.Automatic("balances_overall_clicked", mapOf("source" to SOURCE))

            OnToReceiveClicked -> EventAnalytics.Automatic("balances_to_receive_clicked", mapOf("source" to SOURCE))

            OnToPayClicked -> EventAnalytics.Automatic("balances_to_pay_clicked", mapOf("source" to SOURCE))

            is OnShowSettledChanged -> EventAnalytics.Automatic("settled_groups_toggled", mapOf("is_shown" to isShown))

            is OnFilterChanged -> EventAnalytics.NotTracked

            // Why: only another way to show the groups, never the one already showing.
            is OnDisplayChanged -> EventAnalytics.Manual("groups_display_changed")

            // Why: only a size the grid takes, once per size the Mac's slider passes.
            is OnGridSizeChanged -> EventAnalytics.Manual("groups_grid_size_changed")

            is OnInviteClicked -> EventAnalytics.Automatic("invite_clicked", mapOf("source" to SOURCE))

            is OnLeaveGroupClicked -> EventAnalytics.Automatic("leave_group_clicked", mapOf("source" to SOURCE))

            is OnDeleteGroupClicked -> EventAnalytics.Automatic("delete_group_clicked", mapOf("source" to SOURCE))

            // Why: only an answer to a dialog still open, since Apple's alert also closes itself after its button.
            OnGroupExitConfirmed -> EventAnalytics.Manual("group_exit_confirmed")

            OnGroupExitDismissed -> EventAnalytics.Manual("group_exit_cancelled")

            // Why: a notice that went away, which nobody tapped.
            OnDeletedGroupNoticeShown, OnRecordedPaymentNoticeShown -> EventAnalytics.NotTracked

            OnUndoPaymentClicked -> EventAnalytics.Automatic("payment_undo_clicked", mapOf("source" to SOURCE))

            // Why: only a search that opens, never ⌘K over a dialog nor a late tap over a pushed screen.
            OnSearchOpened, OnSearchTabClicked -> EventAnalytics.Manual("search_opened")

            OnSearchTabLeft -> EventAnalytics.NotTracked

            is OnSearch -> event.toEventAnalytics(source = SOURCE)
        }

    data object OnCreateGroupClicked : GroupsUiEvent

    data object OnJoinWithLinkClicked : GroupsUiEvent

    data class OnGroupClicked(
        val groupId: String,
    ) : GroupsUiEvent

    data object OnProfileClicked : GroupsUiEvent

    // Why: the web's logo, which opens Home as its tab does.
    data object OnBrandClicked : GroupsUiEvent

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

    data object OnRetrySyncClicked : GroupsUiEvent

    // Why: pulled down on Android and iOS.
    data object OnRefresh : GroupsUiEvent

    // Why: "Rachar conta", the FAB on Android, + on iOS, the main button on the web and the Mac.
    data object OnSplitBillClicked : GroupsUiEvent

    // Why: the "Saldo geral" card of the web and the Mac, a button of the page rather than the Saldos tab beside it.
    data object OnOverallBalanceClicked : GroupsUiEvent

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

    // Why: Android's magnifier, Apple's search tab (the Mac's ⌘K too), the web's "Buscar…" or ⌘K.
    data object OnSearchOpened : GroupsUiEvent

    /*
     * Why: iOS's search tab from Grupos, which leaves a screen pushed over the list first, as it does from the other
     * tabs (Navigator.openSearchFromPage).
     */
    data object OnSearchTabClicked : GroupsUiEvent

    /*
     * Why: the search tab (iOS) or item (Android) shows what its results opened, so leaving it for Groups goes back to
     * the groups.
     */
    data object OnSearchTabLeft : GroupsUiEvent

    // Why: what is typed, picked or found in the search open in Grupos' place, as over the other tabs (Route.Search).
    data class OnSearch(
        val event: SearchUiEvent,
    ) : GroupsUiEvent
}
