package com.quare.blitzsplit.feature.editgroup.presentation.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: "Você tem 1 pagamento na fila: Elisa te pagou R$ 50,00." (13.3d)
data class SimplifyQueuedPayment(
    val from: SimplifyName,
    val to: SimplifyName,
    val amountMinor: Long,
    val currency: GroupCurrency,
)
