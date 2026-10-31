package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.GroupExitUiState
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice

data class GroupDetailUiState(
    val header: GroupHeader,
    // Why: the most recent day first.
    val days: List<ExpenseDay>,
    // Why: "Apagar este acerto?", open while not null.
    val paymentToDelete: PaymentRow?,
    // Why: "Pagamento de Ana registrado." with Desfazer, once Registrar pagamento closed over this screen.
    val recordedPayment: PaymentNotice?,
    /*
     * Why: "Esta conta foi apagada", once a bill someone else deleted closed while it was open. State rather than an
     * action, so a screen drawn again only after it is back on top still shows it.
     */
    val showsBillDeletedNotice: Boolean,
    // Why: "Sair do grupo" and "Apagar grupo", and "Grupo apagado".
    val exit: GroupExitUiState,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
) {
    // Why: "Nenhuma despesa ainda", with "Rachar conta" and "Convidar pessoas" in it.
    val isEmpty: Boolean
        get() = isLoaded && days.isEmpty()

    // Why: the empty state has its own "Rachar conta".
    val showsSplitBillButton: Boolean
        get() = isLoaded && days.isNotEmpty()
}
