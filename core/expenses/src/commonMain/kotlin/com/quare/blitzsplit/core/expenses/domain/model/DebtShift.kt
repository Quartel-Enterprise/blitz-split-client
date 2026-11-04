package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: what turning "Simplificar dívidas" on or off changes for one person (13.3), which each UI words in its language:
 * "Não paga mais R$ 118,00 a Bruno. Recebe de Carla, Diego e Elisa."
 */
data class DebtShift(
    val memberId: String,
    // Why: everyone the person pays afterwards, with how much, listed once a payment of theirs is new or changed.
    val pays: List<Debt>,
    // Why: the payments the person made before and no longer makes, with what they were.
    val stopsPaying: List<Debt>,
    // Why: everyone the person receives from afterwards, listed once someone new pays them.
    val receivesFrom: List<String>,
    val stopsReceivingFrom: List<String>,
)
