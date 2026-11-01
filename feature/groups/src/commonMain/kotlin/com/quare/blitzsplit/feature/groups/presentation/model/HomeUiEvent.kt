package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface HomeUiEvent : UiEvent {
    data object OnProfileClicked : HomeUiEvent

    // Why: the chip "Sem resposta do servidor · tentar de novo".
    data object OnRetrySyncClicked : HomeUiEvent

    // Why: pulled down on Android and iOS.
    data object OnRefresh : HomeUiEvent

    // Why: a face of "Saldos com pessoas": settling up with them (4.5) or Recebi (6.5), for the net.
    data class OnPersonClicked(
        val key: String,
        val currency: GroupCurrency,
    ) : HomeUiEvent

    // Why: "Ver saldos" over the faces.
    data object OnSeeBalancesClicked : HomeUiEvent

    // Why: "Te devem" and "Você deve" of "Saldo geral".
    data object OnToReceiveClicked : HomeUiEvent

    data object OnToPayClicked : HomeUiEvent

    // Why: "Rachar conta", Android's extended FAB, iOS's tinted glass item, the web's main button, the Mac's ⌘N.
    data object OnSplitBillClicked : HomeUiEvent

    // Why: the shortcuts: "Ler cupom fiscal", "Registrar pagamento" and "Novo grupo".
    data object OnScanReceiptClicked : HomeUiEvent

    data object OnRecordPaymentClicked : HomeUiEvent

    data object OnCreateGroupClicked : HomeUiEvent

    // Why: the first use's "Entrar com link ou QR".
    data object OnJoinWithLinkClicked : HomeUiEvent

    // Why: a card of "Seus grupos".
    data class OnGroupClicked(
        val groupId: String,
    ) : HomeUiEvent

    // Why: "Ver todos" over "Seus grupos".
    data object OnSeeAllGroupsClicked : HomeUiEvent

    // Why: a row of "Últimas despesas", which opens its Conta (7.3) over its group.
    data class OnBillClicked(
        val groupId: String,
        val expenseId: String,
    ) : HomeUiEvent

    // Why: "Ver tudo" over "Últimas despesas", the Atividade tab.
    data object OnSeeAllBillsClicked : HomeUiEvent

    // Why: the card "Guarde seu código de recuperação": "Ver código", "Conectar conta" and ✕.
    data object OnShowRecoveryCodeClicked : HomeUiEvent

    data object OnConnectAccountClicked : HomeUiEvent

    data object OnRecoveryCodeCardDismissed : HomeUiEvent

    // Why: "Desfazer" on the notice of a payment just recorded from Início.
    data object OnUndoPaymentClicked : HomeUiEvent

    data object OnRecordedPaymentNoticeShown : HomeUiEvent

    data object OnDeletedGroupNoticeShown : HomeUiEvent

    data object OnGroupsTabClicked : HomeUiEvent

    data object OnBalancesTabClicked : HomeUiEvent

    data object OnContactsTabClicked : HomeUiEvent

    data object OnActivityTabClicked : HomeUiEvent
}
