package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency

data class BillDetailUiState(
    // Why: the top bar's title over the bill, as Despesas do grupo is the screen under it.
    val groupName: String,
    val currency: GroupCurrency,
    val description: String,
    val totalMinor: Long,
    // Why: "ontem, 27 de out."; each UI writes the date and the time, after "Hoje" or "Ontem" when it is one of them.
    val spentAt: Long,
    val spentDay: RelativeDay,
    // Why: null when whoever paid is no longer in the group.
    val payer: BillPayer?,
    // Why: "Na fila · sincroniza ao reconectar".
    val isPending: Boolean,
    // Why: "Sua parte", what the bill does to this device's user's balance; null when they aren't in it.
    val myBalanceMinor: Long?,
    val myPartNote: MyPartNote?,
    // Why: "Igual entre 5", "Por valor entre 6".
    val splitMode: SplitMode,
    // Why: everyone in the group in the order they were added, then whoever left it and is still in the bill.
    val people: List<BillShareRow>,
    // Why: "Apagar esta conta?", open while not null.
    val deletion: BillDeletion?,
    val isDeleting: Boolean,
    // Why: nothing is drawn until Room is read.
    val isLoaded: Boolean,
) {
    // Why: the count in "Igual entre 5".
    val participantCount: Int
        get() = people.count { it.shareMinor != null }

    // Why: "Sua parte" only for someone in the bill.
    val showsMyPart: Boolean
        get() = isLoaded && myBalanceMinor != null

    // Why: Editar and Apagar do nothing while the bill is being deleted.
    val areActionsEnabled: Boolean
        get() = isLoaded && !isDeleting
}
