package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics
import com.quare.blitzsplit.core.presentation.analytics.TrackedUiEvent

// Why: the screen these events come from, as their source param says.
private const val SOURCE = "group_detail"

sealed interface GroupDetailUiEvent : TrackedUiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, which each platform also does its own way: where it went is its screen view.
            OnBackClicked -> EventAnalytics.NotTracked

            OnSplitBillClicked -> EventAnalytics.Automatic("split_bill_clicked", mapOf("source" to SOURCE))

            OnInviteClicked -> EventAnalytics.Automatic("invite_clicked", mapOf("source" to SOURCE))

            OnStatsClicked -> EventAnalytics.Automatic("group_stats_clicked")

            is OnTabSelected -> EventAnalytics.Automatic("group_tab_clicked", mapOf("tab" to tab))

            is OnRailTabClicked -> EventAnalytics.Automatic("home_tab_clicked", mapOf("tab" to tab, "source" to SOURCE))

            OnEditGroupClicked -> EventAnalytics.Automatic("edit_group_clicked")

            // Why: a toggle, sent with where it ended up.
            OnPinToggled -> EventAnalytics.Manual("group_pin_toggled")

            OnLeaveGroupClicked -> EventAnalytics.Automatic("leave_group_clicked", mapOf("source" to SOURCE))

            OnDeleteGroupClicked -> EventAnalytics.Automatic("delete_group_clicked", mapOf("source" to SOURCE))

            // Why: only an answer to a dialog still open, since Apple's alert also closes itself after its button.
            OnGroupExitConfirmed -> EventAnalytics.Manual("group_exit_confirmed")

            OnGroupExitDismissed -> EventAnalytics.Manual("group_exit_cancelled")

            OnBackToGroupsClicked -> EventAnalytics.Automatic("group_deleted_back_clicked")

            is OnExpenseClicked -> EventAnalytics.Automatic("bill_clicked", mapOf("source" to SOURCE))

            // Why: a notice that went away, which nobody tapped.
            OnBillDeletedNoticeShown,
            OnRecordedPaymentNoticeShown,
            OnSimplifyConflictDismissed,
            -> EventAnalytics.NotTracked

            OnSettleClicked -> EventAnalytics.Automatic("settle_clicked", mapOf("source" to SOURCE))

            is OnDebtClicked -> EventAnalytics.Automatic("debt_clicked", mapOf("source" to SOURCE))

            is OnPaymentClicked -> EventAnalytics.Automatic("payment_clicked", mapOf("source" to SOURCE))

            OnPaymentDeleteConfirmed -> EventAnalytics.Automatic("payment_delete_confirmed")

            // Why: only a question still open, since Apple's alert also closes itself after Apagar.
            OnPaymentDeleteDismissed -> EventAnalytics.Manual("payment_delete_cancelled")

            OnUndoPaymentClicked -> EventAnalytics.Automatic("payment_undo_clicked", mapOf("source" to SOURCE))

            OnTeachClicked -> EventAnalytics.Automatic("category_teach_clicked")

            OnTeachDismissed -> EventAnalytics.Automatic("category_teach_dismissed")

            is OnTeachWordPicked -> EventAnalytics.Automatic("category_teach_word_picked")

            OnTeachConfirmed -> EventAnalytics.Automatic("category_teach_confirmed")

            OnProtectAccessClicked -> EventAnalytics.Automatic("protect_access_clicked", mapOf("source" to SOURCE))

            OnSimplifyDebtsInfoClicked -> EventAnalytics.Automatic(
                "simplify_debts_info_clicked",
                mapOf("source" to SOURCE),
            )

            OnSimplifyConflictActivityClicked -> EventAnalytics.Automatic(
                "simplify_conflict_activity_clicked",
                mapOf("source" to SOURCE),
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

    // Why: a tab of the home's rail, which Android keeps beside the group when it is drawn alone.
    data class OnRailTabClicked(
        val tab: HomeTab,
    ) : GroupDetailUiEvent

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

    // Why: "Esta conta foi apagada" was shown, so it doesn't show again.
    data object OnBillDeletedNoticeShown : GroupDetailUiEvent

    // Why: "Acertar" on the balance card, which opens Registrar pagamento with whoever this person owes the most.
    data object OnSettleClicked : GroupDetailUiEvent

    // Why: "Acertar", "Recebi" or "Já pagou" on a pair of Saldos, which opens Registrar pagamento for it.
    data class OnDebtClicked(
        val fromMemberId: String,
        val toMemberId: String,
    ) : GroupDetailUiEvent

    // Why: a payment on the list, which asks "Apagar este acerto?".
    data class OnPaymentClicked(
        val paymentId: String,
    ) : GroupDetailUiEvent

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
