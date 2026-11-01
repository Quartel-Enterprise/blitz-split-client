package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: "Carne e carvão do churrasco · Churrasco de sábado · Bruno pagou · R$ 486,00", a bill "Buscar" found.
data class SearchBillHit(
    val id: String,
    val groupId: String,
    val groupName: String,
    val description: String,
    val descriptionMatch: TextMatch,
    val totalMinor: Long,
    val currency: GroupCurrency,
    // Why: who paid, this device's user first, then in the order they were added to the group, as Despesas names them.
    val payers: List<SearchBillPayer>,
    val spentAt: Long,
)
