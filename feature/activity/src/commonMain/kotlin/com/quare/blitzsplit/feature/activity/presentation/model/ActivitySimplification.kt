package com.quare.blitzsplit.feature.activity.presentation.model

/*
 * Why: the detail of "Ana ligou Simplificar dívidas em Viagem Floripa" (13.7): "7 pagamentos viraram 5. Agora Diego te
 * paga R$ 64,00, e Ana não te deve mais.", and "Ver mudanças", only what changed for this device's user.
 */
data class ActivitySimplification(
    val paymentsBefore: Int,
    val paymentsAfter: Int,
    // Why: the group's currency first, then the others by code, the largest change first within each.
    val myChanges: List<ActivityDebtChange>,
) {
    // Why: "Agora Diego te paga R$ 64,00" (or "Agora você paga R$ 64,00 a Bruno"), the first pair that is new, in the group's currency whenever it has one.
    val newPair: ActivityDebtChange?
        get() = myChanges.firstOrNull { it.beforeMinor == 0L && it.afterMinor != 0L }

    // Why: "e Ana não te deve mais" (or "e você não deve mais a Bruno"), the first pair that went, in the group's currency whenever it has one.
    val gonePair: ActivityDebtChange?
        get() = myChanges.firstOrNull { it.afterMinor == 0L && it.beforeMinor != 0L }
}
