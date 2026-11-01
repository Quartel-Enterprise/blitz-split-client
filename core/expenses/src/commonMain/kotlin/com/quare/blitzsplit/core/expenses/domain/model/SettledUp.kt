package com.quare.blitzsplit.core.expenses.domain.model

// Why: "Pagamento de Carla registrado." with Desfazer on the global Saldos, once settling up with her closed.
data class SettledUp(
    // Why: one per group, undone together.
    val settlementIds: List<String>,
    // Why: null for someone no longer in the group.
    val personName: String?,
    // Why: "Acertar" (you paid them) rather than "Recebi" (they paid you).
    val isPaidByMe: Boolean,
)
