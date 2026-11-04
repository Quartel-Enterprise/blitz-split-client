package com.quare.blitzsplit.core.groups.domain.model

/*
 * Why: the group's pairs right before and right after "Simplificar dívidas" changed, as this device had them, which the
 * server copies into Atividade's item and words each push from (13.7): "7 pagamentos viraram 5".
 */
data class SimplifyDebtsChange(
    val before: List<SimplifyDebtsPair>,
    val after: List<SimplifyDebtsPair>,
)
