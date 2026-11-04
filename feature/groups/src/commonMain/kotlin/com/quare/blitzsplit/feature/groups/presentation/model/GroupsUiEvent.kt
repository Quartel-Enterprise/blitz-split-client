package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "groups"

sealed interface GroupsUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            OnCreateGroupClicked -> EventAnalytics.Automatic("create_group_clicked", mapOf("source" to SOURCE))

            OnJoinWithLinkClicked -> EventAnalytics.Automatic("join_with_link_clicked", mapOf("source" to SOURCE))

            is OnGroupClicked -> EventAnalytics.Automatic("group_clicked", mapOf("source" to SOURCE))

            OnProfileClicked -> EventAnalytics.Automatic("profile_avatar_clicked", mapOf("source" to SOURCE))

            // Why: the window drawing the group beside the list or not, never a tap.
            is OnDetailPaneChanged -> EventAnalytics.NotTracked

            OnHomeTabClicked -> EventAnalytics.Automatic("home_tab_clicked", mapOf("tab" to "home", "source" to SOURCE))

            OnBalancesTabClicked -> EventAnalytics.Automatic(
                "home_tab_clicked",
                mapOf(
                    "tab" to "balances",
                    "source" to SOURCE,
                ),
            )

            OnGroupsTabClicked -> EventAnalytics.Automatic(
                "home_tab_clicked",
                mapOf(
                    "tab" to "groups",
                    "source" to SOURCE,
                ),
            )

            OnContactsTabClicked -> EventAnalytics.Automatic(
                "home_tab_clicked",
                mapOf(
                    "tab" to "contacts",
                    "source" to SOURCE,
                ),
            )

            OnActivityTabClicked -> EventAnalytics.Automatic(
                "home_tab_clicked",
                mapOf(
                    "tab" to "activity",
                    "source" to SOURCE,
                ),
            )

            OnRetrySyncClicked -> EventAnalytics.Automatic("sync_retry_clicked", mapOf("source" to SOURCE))

            OnRefresh -> EventAnalytics.Automatic("pull_to_refresh", mapOf("source" to SOURCE))

            OnSplitBillClicked -> EventAnalytics.Automatic("split_bill_clicked", mapOf("source" to SOURCE))

            OnToReceiveClicked -> EventAnalytics.Automatic("balances_to_receive_clicked", mapOf("source" to SOURCE))

            OnToPayClicked -> EventAnalytics.Automatic("balances_to_pay_clicked", mapOf("source" to SOURCE))

            is OnShowSettledChanged -> EventAnalytics.Automatic("settled_groups_toggled", mapOf("is_shown" to isShown))

            is OnFilterChanged -> EventAnalytics.NotTracked

            is OnDisplayChanged -> EventAnalytics.Automatic("groups_display_changed", mapOf("display" to display))

            is OnGridSizeChanged -> EventAnalytics.Automatic("groups_grid_size_changed", mapOf("size" to size))

            is OnInviteClicked -> EventAnalytics.Automatic("invite_clicked", mapOf("source" to SOURCE))

            is OnLeaveGroupClicked -> EventAnalytics.Automatic("leave_group_clicked", mapOf("source" to SOURCE))

            is OnDeleteGroupClicked -> EventAnalytics.Automatic("delete_group_clicked", mapOf("source" to SOURCE))

            // Why: only an answer to a dialog still open, since Apple's alert also closes itself after its button.
            OnGroupExitConfirmed -> EventAnalytics.Manual("group_exit_confirmed")

            OnGroupExitDismissed -> EventAnalytics.Manual("group_exit_cancelled")

            // Why: a notice that went away, which nobody tapped.
            OnDeletedGroupNoticeShown, OnRecordedPaymentNoticeShown -> EventAnalytics.NotTracked

            OnUndoPaymentClicked -> EventAnalytics.Automatic("payment_undo_clicked", mapOf("source" to SOURCE))

            OnSearchOpened -> EventAnalytics.Automatic("search_opened", mapOf("source" to SOURCE))

            OnSearchTabLeft -> EventAnalytics.NotTracked

            is OnSearch -> event.toEventAnalytics(source = SOURCE)
        }

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

    // Why: iOS's search tab shows what its results opened, so leaving it for Groups also goes back to the groups.
    data object OnSearchTabLeft : GroupsUiEvent

    // Why: what is typed, picked or found in the search open in Grupos' place, as over the other tabs (Route.Search).
    data class OnSearch(
        val event: SearchUiEvent,
    ) : GroupsUiEvent
}
