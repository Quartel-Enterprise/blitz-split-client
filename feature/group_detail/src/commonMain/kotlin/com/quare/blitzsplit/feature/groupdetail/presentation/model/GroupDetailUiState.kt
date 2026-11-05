package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.expenses.domain.model.SimplifyDebtsConflictNotice
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
    /*
     * Why: the account's copy isn't whole yet (a first download, ObserveFirstDownload not null, or not read yet), so an
     * empty Despesas or Saldos may only be bills still coming: skeletons instead of "Nenhuma despesa ainda".
     */
    val awaitsFullSync: Boolean,
    // Why: what is still coming waits for the internet, said over the skeletons instead of a bar that runs.
    val isWaitingForInternet: Boolean,
    // Why: "Ensinar ao mapa?" (9.3e) after a bill of this group was saved in a category picked by hand.
    val teach: CategoryTeach?,
    // Why: the home tab the screen was opened from, which Android's rail beside it selects.
    val homeTab: HomeTab,
    // Why: "Você entrou como Bruno", right after joining the group through its link (3.3).
    val joinedAs: String?,
    // Why: "Proteja seu acesso" under it, while the account has no Google or Apple.
    val isAccountUnprotected: Boolean,
    // Why: Saldos' "Dívidas simplificadas" (13.4) while the group's "Simplificar dívidas" is on; null while it is off.
    val simplification: DebtSimplificationSummary?,
    // Why: "Ana desligou depois de você" (13.1e), when this screen is the group's on top as it comes.
    val simplifyConflict: SimplifyDebtsConflictNotice?,
) {
    // Why: the card over the pairs, only while there is any to explain.
    val showsSimplificationCard: Boolean
        get() = simplification != null && (myDebts.isNotEmpty() || otherDebts.isNotEmpty())

    val showsProtectAccess: Boolean
        get() = joinedAs != null && isAccountUnprotected

    // Why: Despesas' "Nenhuma despesa ainda", with "Rachar conta" and "Convidar pessoas" in it; never a guess.
    val isEmpty: Boolean
        get() = isLoaded && !awaitsFullSync && days.isEmpty()

    // Why: Despesas' skeleton rows, while no bill is on the device yet and some may still be coming.
    val isLoadingBills: Boolean
        get() = isLoaded && awaitsFullSync && days.isEmpty()

    // Why: Saldos' "Ninguém deve nada ainda" / "Os saldos aparecem quando a primeira despesa for lançada."
    val isBalancesEmpty: Boolean
        get() = isLoaded && !awaitsFullSync && hasNoBalances

    // Why: Saldos' skeleton rows, while nobody owes anything on the device yet and bills may still be coming.
    val isLoadingBalances: Boolean
        get() = isLoaded && awaitsFullSync && hasNoBalances

    // Why: "Seu saldo neste grupo" shimmers in its slot, rather than going, until Room is read and the copy is whole.
    val isMyBalanceLoading: Boolean
        get() = header.myBalance == null && (!isLoaded || awaitsFullSync)

    private val hasNoBalances: Boolean
        get() = !hasBills && myDebts.isEmpty() && otherDebts.isEmpty()

    // Why: "As dívidas de cada par já vêm compensadas…" under Saldos' lists, while there is any.
    val showsDebtNote: Boolean
        get() = myDebts.isNotEmpty() || otherDebts.isNotEmpty()

    /*
     * Why: "Rachar conta" stays the group's main button on every tab, so it never changes under the person (Membros
     * keeps "Adicionar pessoa" beside its link); only the empty Despesas leaves it to its own. Over the skeletons
     * too, so a bill can still be split offline while the account's copy waits for the internet.
     */
    val showsSplitBillButton: Boolean
        get() = isLoaded && (tab != GroupTab.EXPENSES || days.isNotEmpty() || isLoadingBills)
}
