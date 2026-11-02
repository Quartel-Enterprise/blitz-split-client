package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab

// Why: the group's screen, its header over Despesas, Saldos or Membros, whichever tab is picked.
data class GroupDetailUiState(
    val header: GroupHeader,
    // Why: the tab picked under the header; picking another only swaps what is drawn under it.
    val tab: GroupTab,
    // Why: Despesas, the most recent day first.
    val days: List<ExpenseDay>,
    // Why: "Apagar este acerto?", open while not null.
    val paymentToDelete: PaymentRow?,
    // Why: Saldos' "Com você", what this person owes first, then what they're owed, the largest first in each.
    val myDebts: List<DebtRow>,
    // Why: Saldos' "Entre os outros", the largest first.
    val otherDebts: List<DebtRow>,
    // Why: Saldos' "Ninguém deve nada ainda" before the group's first bill.
    val hasBills: Boolean,
    // Why: "Pagamento de Ana registrado." with Desfazer, once Registrar pagamento closed over this screen.
    val recordedPayment: PaymentNotice?,
    /*
     * Why: "Esta conta foi apagada", once a bill someone else deleted closed while it was open. State rather than an
     * action, so a screen drawn again only after it is back on top still shows it.
     */
    val showsBillDeletedNotice: Boolean,
    // Why: "Sair do grupo" and "Apagar grupo", and "Grupo apagado".
    val exit: GroupExitState,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
    // Why: the home tab the screen was opened from, which Android's rail beside it selects.
    val homeTab: HomeTab,
    // Why: "Você entrou como Bruno", right after joining the group through its link (3.3).
    val joinedAs: String?,
    // Why: "Proteja seu acesso" under it, while the account has no Google or Apple.
    val isAccountUnprotected: Boolean,
    // Why: Atividade's unread badge on Android's rail beside the screen.
    val activityUnreadCount: Int,
) {
    val showsProtectAccess: Boolean
        get() = joinedAs != null && isAccountUnprotected

    // Why: Despesas' "Nenhuma despesa ainda", with "Rachar conta" and "Convidar pessoas" in it.
    val isEmpty: Boolean
        get() = isLoaded && days.isEmpty()

    // Why: Saldos' "Ninguém deve nada ainda" / "Os saldos aparecem quando a primeira despesa for lançada."
    val isBalancesEmpty: Boolean
        get() = isLoaded && !hasBills && myDebts.isEmpty() && otherDebts.isEmpty()

    // Why: "As dívidas de cada par já vêm compensadas…" under Saldos' lists, while there is any.
    val showsDebtNote: Boolean
        get() = myDebts.isNotEmpty() || otherDebts.isNotEmpty()

    // Why: the empty Despesas has its own "Rachar conta", and Membros has "Adicionar pessoa" instead.
    val showsSplitBillButton: Boolean
        get() = isLoaded &&
            when (tab) {
                GroupTab.EXPENSES -> days.isNotEmpty()
                GroupTab.BALANCES -> true
                GroupTab.MEMBERS -> false
            }
}
