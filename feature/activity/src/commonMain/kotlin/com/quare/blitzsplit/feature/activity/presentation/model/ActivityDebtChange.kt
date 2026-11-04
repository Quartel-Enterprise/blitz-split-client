package com.quare.blitzsplit.feature.activity.presentation.model

/*
 * Why: one line of "Ver mudanças" of "Simplificar dívidas" (13.7b), what someone owed this device's user before and
 * after, positive when they owe the user: "Diego · não te devia → te deve R$ 64,00".
 */
data class ActivityDebtChange(
    // Why: null for nobody the item knows the name of, whom each UI calls "Ex-membro".
    val name: String?,
    val beforeMinor: Long,
    val afterMinor: Long,
)
