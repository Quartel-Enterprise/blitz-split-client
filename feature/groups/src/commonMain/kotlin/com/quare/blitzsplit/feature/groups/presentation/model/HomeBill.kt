package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: a row of Início's "Últimas despesas": "Mercado · Viagem Floripa · Você pagou · R$ 312,00 · +R$ 249,60".
data class HomeBill(
    val id: String,
    val groupId: String,
    val groupName: String,
    val description: String,
    val totalMinor: Long,
    val currency: GroupCurrency,
    // Why: who paid, this device's user first, as Despesas names them.
    val payers: List<SearchBillPayer>,
    // Why: what the bill does to this device's user's balance: positive "a receber", negative "você deve".
    val myBalanceMinor: Long,
    // Why: "na fila" and the orange dot: saved here and not on the server yet.
    val isPending: Boolean,
)
