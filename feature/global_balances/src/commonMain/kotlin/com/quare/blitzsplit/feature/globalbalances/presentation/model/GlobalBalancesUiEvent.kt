package com.quare.blitzsplit.feature.globalbalances.presentation.model

import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface GlobalBalancesUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: only another filter, never the one already picked.
            is OnFilterChanged -> EventAnalytics.Manual("balances_filter_selected")

            // Why: a toggle, sent with where it ended up.
            is OnPersonExpandToggled -> EventAnalytics.Manual("balances_person_expand_toggled")

            // Why: only someone the filter lists, never a stale tap the screen ignores.
            is OnPersonSelected -> EventAnalytics.Manual("balances_person_selected")

            is OnPersonClicked -> EventAnalytics.Automatic("person_clicked", mapOf("source" to GLOBAL_BALANCES_SOURCE))

            is OnGroupClicked -> EventAnalytics.Automatic("group_clicked", mapOf("source" to GLOBAL_BALANCES_SOURCE))

            is OnSettleClicked -> EventAnalytics.Automatic("settle_clicked", mapOf("source" to GLOBAL_BALANCES_SOURCE))

            is OnReceivedClicked -> EventAnalytics.Automatic(
                "received_clicked",
                mapOf("source" to GLOBAL_BALANCES_SOURCE),
            )

            is OnChargeClicked -> EventAnalytics.Automatic("charge_clicked", mapOf("source" to GLOBAL_BALANCES_SOURCE))

            OnEmptyActionClicked -> EventAnalytics.Automatic("balances_empty_action_clicked")

            // Why: only a tap that opens Split a bill, never a late one over another screen or with no group.
            OnSplitBillClicked -> EventAnalytics.Manual("split_bill_clicked")

            /*
             * Why: a tab of the home's bar, rail or sidebar, which each platform routes through whichever screen is in front:
             * where it went is its screen view.
             */
            OnHomeTabClicked,
            OnGroupsTabClicked,
            OnContactsTabClicked,
            -> EventAnalytics.NotTracked

            OnProfileClicked -> EventAnalytics.Automatic(
                "profile_avatar_clicked",
                mapOf("source" to GLOBAL_BALANCES_SOURCE),
            )

            // Why: the web's logo, which opens Home; the tabs it stands beside are not tracked, their screen view is.
            OnBrandClicked -> EventAnalytics.Automatic("brand_clicked", mapOf("source" to GLOBAL_BALANCES_SOURCE))

            // Why: only a search that opens, never one refused over Cobrar.
            OnSearchClicked -> EventAnalytics.Manual("search_opened")

            OnUndoPaymentClicked -> EventAnalytics.Automatic(
                "payment_undo_clicked",
                mapOf("source" to GLOBAL_BALANCES_SOURCE),
            )

            // Why: a notice that went away, which nobody tapped.
            OnRecordedPaymentNoticeShown -> EventAnalytics.NotTracked
        }

    // Why: "Tudo", "Você deve" or "Te devem".
    data class OnFilterChanged(
        val filter: BalancesFilter,
    ) : GlobalBalancesUiEvent

    // Why: "Por grupo" opened or closed on a row.
    data class OnPersonExpandToggled(
        val personId: String,
    ) : GlobalBalancesUiEvent

    // Why: a row picked in a list and detail, whose person the pane beside the list then shows.
    data class OnPersonSelected(
        val personId: String,
    ) : GlobalBalancesUiEvent

    // Why: the face or the name, which open the person's profile (11.2), as Contatos does.
    data class OnPersonClicked(
        val personId: String,
    ) : GlobalBalancesUiEvent

    // Why: a group's badge or name in "Por grupo", which opens the group.
    data class OnGroupClicked(
        val groupId: String,
    ) : GlobalBalancesUiEvent

    // Why: "Acertar", someone you owe.
    data class OnSettleClicked(
        val personId: String,
    ) : GlobalBalancesUiEvent

    // Why: "Recebi", someone who owes you paid outside the app.
    data class OnReceivedClicked(
        val personId: String,
    ) : GlobalBalancesUiEvent

    data class OnChargeClicked(
        val personId: String,
    ) : GlobalBalancesUiEvent

    // Why: the empty state's button: "Ver quem te deve", "Ver quem você deve" or "Ver grupos".
    data object OnEmptyActionClicked : GlobalBalancesUiEvent

    // Why: Android's "Rachar conta", the FAB (or the rail's button) every home tab has.
    data object OnSplitBillClicked : GlobalBalancesUiEvent

    data object OnHomeTabClicked : GlobalBalancesUiEvent

    data object OnGroupsTabClicked : GlobalBalancesUiEvent

    data object OnContactsTabClicked : GlobalBalancesUiEvent

    data object OnProfileClicked : GlobalBalancesUiEvent

    // Why: the web's logo, which opens Home as its tab does.
    data object OnBrandClicked : GlobalBalancesUiEvent

    // Why: "Buscar" (5.2), the same search as Grupos', over Saldos: the magnifier, the web's ⌘K.
    data object OnSearchClicked : GlobalBalancesUiEvent

    // Why: "Desfazer" on the notice of a settling up just recorded.
    data object OnUndoPaymentClicked : GlobalBalancesUiEvent

    // Why: the notice went away without Desfazer.
    data object OnRecordedPaymentNoticeShown : GlobalBalancesUiEvent
}
