package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

// Why: the screen these events come from, as their source param says; the ViewModel's own events say it too.
internal const val GROUP_DETAIL_SOURCE = "group_detail"

sealed interface GroupDetailUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            OnSplitBillClicked -> EventAnalytics.Automatic("split_bill_clicked", mapOf("source" to GROUP_DETAIL_SOURCE))

            OnInviteClicked -> EventAnalytics.Automatic("invite_clicked", mapOf("source" to GROUP_DETAIL_SOURCE))

            OnStatsClicked -> EventAnalytics.Automatic("group_stats_clicked")

            is OnTabSelected -> EventAnalytics.Automatic("group_tab_clicked", mapOf("tab" to tab))

            // Why: no tap, the tab already picked coming back with the page.
            is OnTabRestored -> EventAnalytics.NotTracked

            /*
             * Why: a tab of the home's bar, rail or sidebar, which each platform routes through whichever screen is in front:
             * where it went is its screen view.
             */
            is OnRailTabClicked -> EventAnalytics.NotTracked

            // Why: only a search that opens, never a late tap over what the group opened.
            OnRailSearchClicked -> EventAnalytics.Manual("search_opened")

            OnEditGroupClicked -> EventAnalytics.Automatic("edit_group_clicked")

            // Why: a toggle, sent with where it ended up.
            OnPinToggled -> EventAnalytics.Manual("group_pin_toggled")

            OnLeaveGroupClicked -> EventAnalytics.Automatic(
                "leave_group_clicked",
                mapOf("source" to GROUP_DETAIL_SOURCE),
            )

            OnDeleteGroupClicked -> EventAnalytics.Automatic(
                "delete_group_clicked",
                mapOf("source" to GROUP_DETAIL_SOURCE),
            )

            // Why: only an answer to a dialog still open, since Apple's alert also closes itself after its button.
            OnGroupExitConfirmed -> EventAnalytics.Manual("group_exit_confirmed")

            OnGroupExitDismissed -> EventAnalytics.Manual("group_exit_cancelled")

            // Why: only while "Grupo apagado" shows, since Apple's alert also closes itself after its button.
            OnBackToGroupsClicked -> EventAnalytics.Manual("group_deleted_back_clicked")

            is OnExpenseClicked -> EventAnalytics.Automatic("bill_clicked", mapOf("source" to GROUP_DETAIL_SOURCE))

            // Why: only once Editar conta opened, as Conta counts it: never for a bill gone or a group covered.
            is OnExpenseEditClicked -> EventAnalytics.Manual("bill_edit_clicked")

            is OnExpenseShareClicked -> EventAnalytics.Manual("bill_share_clicked")

            // Why: only when the question opens, not a second tap before it draws.
            is OnExpenseDeleteClicked -> EventAnalytics.Manual("bill_delete_clicked")

            // Why: only an answer to a question still open and not deleting yet, as Conta counts it.
            OnExpenseDeleteConfirmed -> EventAnalytics.Manual("bill_delete_confirmed")

            OnExpenseDeleteDismissed -> EventAnalytics.Manual("bill_delete_cancelled")

            // Why: a notice that went away, which nobody tapped.
            OnBillDeletedNoticeShown,
            OnRecordedPaymentNoticeShown,
            OnSimplifyConflictDismissed,
            -> EventAnalytics.NotTracked

            OnSettleClicked -> EventAnalytics.Automatic("settle_clicked", mapOf("source" to GROUP_DETAIL_SOURCE))

            is OnDebtClicked -> EventAnalytics.Automatic("debt_clicked", mapOf("source" to GROUP_DETAIL_SOURCE))

            is OnPaymentClicked -> EventAnalytics.Automatic("payment_clicked", mapOf("source" to GROUP_DETAIL_SOURCE))

            is OnPaymentDeleteClicked -> EventAnalytics.Automatic(
                "payment_delete_clicked",
                mapOf("source" to GROUP_DETAIL_SOURCE),
            )

            // Why: closing a sheet that only showed information.
            OnPaymentDetailsDismissed -> EventAnalytics.NotTracked

            // Why: only an answer to a question still open, as its cancellation.
            OnPaymentDeleteConfirmed -> EventAnalytics.Manual("payment_delete_confirmed")

            // Why: only a question still open, since Apple's alert also closes itself after Apagar.
            OnPaymentDeleteDismissed -> EventAnalytics.Manual("payment_delete_cancelled")

            OnUndoPaymentClicked -> EventAnalytics.Automatic(
                "payment_undo_clicked",
                mapOf("source" to GROUP_DETAIL_SOURCE),
            )

            OnTeachClicked -> EventAnalytics.Automatic("category_teach_clicked")

            // Why: only an offer or a question still open, never one saving, nor a second tap on Ensinar.
            OnTeachDismissed -> EventAnalytics.Manual("category_teach_dismissed")

            is OnTeachWordPicked -> EventAnalytics.Automatic("category_teach_word_picked")

            OnTeachConfirmed -> EventAnalytics.Manual("category_teach_confirmed")

            OnProtectAccessClicked -> EventAnalytics.Automatic(
                "protect_access_clicked",
                mapOf("source" to GROUP_DETAIL_SOURCE),
            )

            OnSimplifyDebtsInfoClicked -> EventAnalytics.Automatic(
                "simplify_debts_info_clicked",
                mapOf("source" to GROUP_DETAIL_SOURCE),
            )

            OnSimplifyConflictActivityClicked -> EventAnalytics.Automatic(
                "simplify_conflict_activity_clicked",
                mapOf("source" to GROUP_DETAIL_SOURCE),
            )
        }

    data object OnBackClicked : GroupDetailUiEvent

    // Why: "Rachar conta", the button and the empty state's.
    data object OnSplitBillClicked : GroupDetailUiEvent

    // Why: "Convidar pessoas", the top bar's and the empty state's.
    data object OnInviteClicked : GroupDetailUiEvent

    // Why: "Estatísticas" in the top bar, next to "Convidar pessoas".
    data object OnStatsClicked : GroupDetailUiEvent

    // Why: Despesas, Saldos or Membros under the header, and the faces under the name (Membros).
    data class OnTabSelected(
        val tab: GroupTab,
    ) : GroupDetailUiEvent

    /*
     * Why: the web draws only the top page, so a page the group opens (Estatísticas, a person's profile) drops the
     * group's, and back draws it anew: the tab picked before comes back with it.
     */
    data class OnTabRestored(
        val tab: GroupTab,
    ) : GroupDetailUiEvent

    // Why: a tab of the home's rail, which Android keeps beside the group when it is drawn alone.
    data class OnRailTabClicked(
        val tab: HomeTab,
    ) : GroupDetailUiEvent

    // Why: the rail's last item, "Buscar", as on the home's rail: the search over the tab the group was opened from.
    data object OnRailSearchClicked : GroupDetailUiEvent

    // Why: "Editar grupo" in the ⋮ menu.
    data object OnEditGroupClicked : GroupDetailUiEvent

    // Why: "Fixar na Início" or "Desafixar da Início" in the ⋮ menu.
    data object OnPinToggled : GroupDetailUiEvent

    // Why: "Sair do grupo" in the ⋮ menu.
    data object OnLeaveGroupClicked : GroupDetailUiEvent

    // Why: "Apagar grupo" in the ⋮ menu and on "Todo mundo acertou".
    data object OnDeleteGroupClicked : GroupDetailUiEvent

    // Why: the main button of the dialog they open: "Sair", "Apagar para todos", "Acertar" or "Ver saldos".
    data object OnGroupExitConfirmed : GroupDetailUiEvent

    // Why: "Cancelar" or "Agora não", or the dialog dismissed.
    data object OnGroupExitDismissed : GroupDetailUiEvent

    // Why: "Voltar para Grupos" on "Grupo apagado".
    data object OnBackToGroupsClicked : GroupDetailUiEvent

    // Why: a bill on the list, which opens "Conta".
    data class OnExpenseClicked(
        val expenseId: String,
    ) : GroupDetailUiEvent

    // Why: "Editar" in a bill's ⋮ on the list, Conta's Editar without opening it first.
    data class OnExpenseEditClicked(
        val expenseId: String,
    ) : GroupDetailUiEvent

    // Why: "Compartilhar" in a bill's ⋮ on the list, which opens "Compartilhar conta".
    data class OnExpenseShareClicked(
        val expenseId: String,
    ) : GroupDetailUiEvent

    // Why: "Apagar" in a bill's ⋮ on the list, which asks "Apagar esta conta?" first, as Conta does.
    data class OnExpenseDeleteClicked(
        val expenseId: String,
    ) : GroupDetailUiEvent

    // Why: "Apagar" on "Apagar esta conta?".
    data object OnExpenseDeleteConfirmed : GroupDetailUiEvent

    // Why: "Cancelar" on "Apagar esta conta?", or the dialog closed some other way.
    data object OnExpenseDeleteDismissed : GroupDetailUiEvent

    // Why: "Esta conta foi apagada" was shown, so it doesn't show again.
    data object OnBillDeletedNoticeShown : GroupDetailUiEvent

    // Why: "Acertar" on the balance card, which opens Registrar pagamento with whoever this person owes the most.
    data object OnSettleClicked : GroupDetailUiEvent

    // Why: "Acertar", "Recebi" or "Já pagou" on a pair of Saldos, which opens Registrar pagamento for it.
    data class OnDebtClicked(
        val fromMemberId: String,
        val toMemberId: String,
    ) : GroupDetailUiEvent

    // Why: a payment on the list, which opens its details.
    data class OnPaymentClicked(
        val paymentId: String,
    ) : GroupDetailUiEvent

    // Why: "Apagar" in a payment's ⋮ or in its details, which asks "Apagar este acerto?".
    data class OnPaymentDeleteClicked(
        val paymentId: String,
    ) : GroupDetailUiEvent

    data object OnPaymentDetailsDismissed : GroupDetailUiEvent

    data object OnPaymentDeleteConfirmed : GroupDetailUiEvent

    data object OnPaymentDeleteDismissed : GroupDetailUiEvent

    // Why: "Desfazer" on "Pagamento de Ana registrado.".
    data object OnUndoPaymentClicked : GroupDetailUiEvent

    // Why: the notice went away without Desfazer.
    data object OnRecordedPaymentNoticeShown : GroupDetailUiEvent

    // Why: "Ensinar" on "Ensinar ao mapa?" (9.3e), which asks which word of the title.
    data object OnTeachClicked : GroupDetailUiEvent

    // Why: "Agora não", or "Cancelar" on "Ensinar ao mapa".
    data object OnTeachDismissed : GroupDetailUiEvent

    data class OnTeachWordPicked(
        val key: String,
    ) : GroupDetailUiEvent

    data object OnTeachConfirmed : GroupDetailUiEvent

    // Why: "Proteja seu acesso", which opens the profile, where the code is kept and Google or Apple connected.
    data object OnProtectAccessClicked : GroupDetailUiEvent

    // Why: "Como funciona" on Saldos' "Dívidas simplificadas" (13.4).
    data object OnSimplifyDebtsInfoClicked : GroupDetailUiEvent

    // Why: the notice of 13.1e went away.
    data object OnSimplifyConflictDismissed : GroupDetailUiEvent

    // Why: its "Ver na Atividade".
    data object OnSimplifyConflictActivityClicked : GroupDetailUiEvent
}
