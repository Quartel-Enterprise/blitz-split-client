package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillItem

// Why: one line of "Itens da conta": the item, the faces of who consumed it, in red while it has nobody (7.5c).
data class ItemsItemRow(
    val item: BillItem,
    val consumers: List<BillPerson>,
    val isLoose: Boolean,
)
