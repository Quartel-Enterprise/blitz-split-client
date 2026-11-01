package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice

// Why: "Saldos", the group's tab with who owes whom, each pair once and already netted.
data class BalancesUiState(
    val header: GroupHeader,
    // Why: "Com você", what this person owes first, then what they're owed, the largest first in each.
    val myDebts: List<DebtRow>,
    // Why: "Entre os outros", the largest first.
    val otherDebts: List<DebtRow>,
    // Why: "Ninguém deve nada ainda" before the group's first bill.
    val hasBills: Boolean,
    // Why: "Pagamento de Ana registrado." with Desfazer, once Registrar pagamento closed over this screen.
    val recordedPayment: PaymentNotice?,
    // Why: "Sair do grupo" and "Apagar grupo", and "Grupo apagado".
    val exit: GroupExitState,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
) {
    // Why: "Ninguém deve nada ainda" / "Os saldos aparecem quando a primeira despesa for lançada."
    val isEmpty: Boolean
        get() = isLoaded && !hasBills && myDebts.isEmpty() && otherDebts.isEmpty()

    // Why: "As dívidas de cada par já vêm compensadas…" under the lists, while there is any.
    val showsDebtNote: Boolean
        get() = myDebts.isNotEmpty() || otherDebts.isNotEmpty()

    val showsSplitBillButton: Boolean
        get() = isLoaded
}
