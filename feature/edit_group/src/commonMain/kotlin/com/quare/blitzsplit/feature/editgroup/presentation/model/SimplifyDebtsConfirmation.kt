package com.quare.blitzsplit.feature.editgroup.presentation.model

/*
 * Why: "Simplificar dívidas?" (13.3) or "Desligar Simplificar dívidas?" (13.3b), as the group's pairs stand now:
 * "7 pagamentos → 5 pagamentos" (one row per currency) and what changes for each person, this device's user first.
 */
data class SimplifyDebtsConfirmation(
    val isTurningOn: Boolean,
    val groupName: String,
    // Why: "7 pagamentos → 5 pagamentos" for each currency, since each one simplifies on its own.
    val currencies: List<SimplifyCurrencyChange>,
    // Why: everyone something changes for when turning on; only this device's user when turning off (13.3b).
    val people: List<SimplifyPersonChange>,
    // Why: "Mais 5 pessoas mudam.", turning off; 0 turning on, whose list names everyone.
    val othersChangingCount: Int,
    // Why: the payments recorded here and not up yet, which the simplification counts with (13.3d).
    val queuedPayments: List<SimplifyQueuedPayment>,
) {
    // Why: "Futebol de quinta já está no mínimo. Ligue mesmo assim…" with "Ligar mesmo assim" (13.3c).
    val isAlreadyMinimal: Boolean
        get() = isTurningOn && currencies.all { it.isAlreadyMinimal }

    // Why: "A confirmação lembra de avisá-lo": a guest someone now pays or who pays someone new.
    val hasGuestChanging: Boolean
        get() = people.any { it.isGuest }
}
