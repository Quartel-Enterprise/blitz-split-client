package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: "Quanto fica para cada um": "R$ 120,00 em itens + R$ 14,50 de serviço e couvert".
data class ItemsPersonRow(
    val person: BillPerson,
    val itemsMinor: Long,
    val feesMinor: Long,
    val totalMinor: Long,
)
