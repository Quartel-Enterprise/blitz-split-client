package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.PaymentNotice
import com.quare.blitzsplit.core.model.HomeTab

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
    val exit: GroupExitState,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
    // Why: "Ensinar ao mapa?" (9.3e) after a bill of this group was saved in a category picked by hand.
    val teach: CategoryTeach?,
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

    // Why: "Nenhuma despesa ainda", with "Rachar conta" and "Convidar pessoas" in it.
    val isEmpty: Boolean
        get() = isLoaded && days.isEmpty()

    // Why: the empty state has its own "Rachar conta".
    val showsSplitBillButton: Boolean
        get() = isLoaded && days.isNotEmpty()
}
