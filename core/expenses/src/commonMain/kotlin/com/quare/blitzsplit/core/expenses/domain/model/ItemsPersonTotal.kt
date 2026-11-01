package com.quare.blitzsplit.core.expenses.domain.model

// Why: "Quanto fica para cada um": what one person consumed and what the service, couvert and difference add.
data class ItemsPersonTotal(
    val memberId: String,
    val itemsMinor: Long,
    val serviceMinor: Long,
    val couvertMinor: Long,
    val extraMinor: Long,
) {
    val totalMinor: Long
        get() = itemsMinor + serviceMinor + couvertMinor + extraMinor

    // Why: "R$ 14,50 de serviço e couvert", the difference shared with them included.
    val feesMinor: Long
        get() = serviceMinor + couvertMinor + extraMinor
}
