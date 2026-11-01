package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface BalancesUiEvent : UiEvent {
    data object OnBackClicked : BalancesUiEvent

    // Why: "Rachar conta", the same as on Despesas.
    data object OnSplitBillClicked : BalancesUiEvent

    // Why: "Convidar pessoas" in the top bar.
    data object OnInviteClicked : BalancesUiEvent

    // Why: "Estatísticas" in the top bar.
    data object OnStatsClicked : BalancesUiEvent

    // Why: the "Despesas" tab.
    data object OnExpensesTabClicked : BalancesUiEvent

    // Why: the "Membros" tab and the faces under the name.
    data object OnMembersClicked : BalancesUiEvent

    // Why: a tab of the home's rail, which Android keeps beside the group when it is drawn alone.
    data class OnRailTabClicked(
        val tab: HomeTab,
    ) : BalancesUiEvent

    // Why: "Editar grupo" in the ⋮ menu.
    data object OnEditGroupClicked : BalancesUiEvent

    // Why: "Fixar na Início" or "Desafixar da Início" in the ⋮ menu.
    data object OnPinToggled : BalancesUiEvent

    // Why: "Sair do grupo" in the ⋮ menu.
    data object OnLeaveGroupClicked : BalancesUiEvent

    // Why: "Apagar grupo" in the ⋮ menu and on "Todo mundo acertou".
    data object OnDeleteGroupClicked : BalancesUiEvent

    // Why: the main button of the dialog they open: "Sair", "Apagar para todos", "Acertar" or "Ver saldos".
    data object OnGroupExitConfirmed : BalancesUiEvent

    // Why: "Cancelar" or "Agora não", or the dialog dismissed.
    data object OnGroupExitDismissed : BalancesUiEvent

    // Why: "Voltar para Grupos" on "Grupo apagado".
    data object OnBackToGroupsClicked : BalancesUiEvent

    // Why: "Acertar" on the balance card, which opens Registrar pagamento with whoever this person owes the most.
    data object OnSettleClicked : BalancesUiEvent

    // Why: "Acertar", "Recebi" or "Já pagou" on a pair, which opens Registrar pagamento for it.
    data class OnDebtClicked(
        val fromMemberId: String,
        val toMemberId: String,
    ) : BalancesUiEvent

    // Why: "Desfazer" on "Pagamento de Ana registrado.".
    data object OnUndoPaymentClicked : BalancesUiEvent

    // Why: the notice went away without Desfazer.
    data object OnRecordedPaymentNoticeShown : BalancesUiEvent
}
