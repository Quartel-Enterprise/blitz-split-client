package com.quare.blitzsplit.feature.editgroup.presentation.model

// Why: "R$ 203,80 a Bruno" in "O que muda" (13.3).
data class SimplifyAmount(
    val to: SimplifyName,
    val amountMinor: Long,
)
