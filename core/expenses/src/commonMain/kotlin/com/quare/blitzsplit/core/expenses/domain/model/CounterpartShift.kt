package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: one line of Atividade's "Ver mudanças" (13.7b), what someone owed this device's user before and after, positive
 * when they owe the user and negative when the user owes them: "Diego · não te devia → te deve R$ 64,00".
 */
data class CounterpartShift(
    val memberId: String,
    val beforeMinor: Long,
    val afterMinor: Long,
)
