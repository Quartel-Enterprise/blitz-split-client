package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: "Ana desligou depois de você. Vale a última mudança: Simplificar dívidas está desligado." (13.1e), with "Ver
 * na Atividade", once this device's change lost to a later one.
 */
data class SimplifyDebtsConflictNotice(
    // Why: null for someone no longer in the group, whom each UI calls "Alguém".
    val changedByName: String?,
    // Why: how it stands now, the later change's.
    val simplifiesDebts: Boolean,
)
