package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface GroupDetailUiEvent : UiEvent {
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
}
