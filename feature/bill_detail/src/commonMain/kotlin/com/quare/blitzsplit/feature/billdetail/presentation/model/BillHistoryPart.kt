package com.quare.blitzsplit.feature.billdetail.presentation.model

// Why: "Carla R$ 112,00 → R$ 98,00" under "mudou a divisão"; each UI says "Você" for isMe, "Ex-membro" without a name.
data class BillHistoryPart(
    val name: String?,
    val isMe: Boolean,
    // Why: null for someone out of the split on that side.
    val beforeMinor: Long?,
    val afterMinor: Long?,
)
