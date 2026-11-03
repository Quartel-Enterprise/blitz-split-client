package com.quare.blitzsplit.core.expenses.domain.model

// Why: one line of a receipt read (7.1e), which becomes one of Itens' items with nobody marked yet.
data class ReceiptItem(
    val name: String,
    val quantity: Long,
    val unitPriceMinor: Long,
)
