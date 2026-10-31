package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface GroupDetailUiEvent : UiEvent {
    data object OnBackClicked : GroupDetailUiEvent

    // Why: "Rachar conta", the button and the empty state's.
    data object OnSplitBillClicked : GroupDetailUiEvent

    // Why: "Convidar pessoas", the top bar's and the empty state's.
    data object OnInviteClicked : GroupDetailUiEvent

    // Why: the "Membros" tab and the faces under the name.
    data object OnMembersClicked : GroupDetailUiEvent

    // Why: the "Saldos" tab.
    data object OnBalancesTabClicked : GroupDetailUiEvent

    // Why: "Editar grupo" in the ⋮ menu.
    data object OnEditGroupClicked : GroupDetailUiEvent

    // Why: "Sair do grupo" in the ⋮ menu, which isn't built yet.
    data object OnLeaveGroupClicked : GroupDetailUiEvent

    // Why: "Apagar grupo" in the ⋮ menu and on "Todo mundo acertou", which isn't built yet.
    data object OnDeleteGroupClicked : GroupDetailUiEvent

    // Why: a bill on the list, which opens "Conta".
    data class OnExpenseClicked(
        val expenseId: String,
    ) : GroupDetailUiEvent

    // Why: "Esta conta foi apagada" was shown, so it doesn't show again.
    data object OnBillDeletedNoticeShown : GroupDetailUiEvent

    // Why: "Acertar" on the balance card, which opens Registrar pagamento with whoever this person owes the most.
    data object OnSettleClicked : GroupDetailUiEvent

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
}
