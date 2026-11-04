package com.quare.blitzsplit.feature.registerpayment.presentation.model

/*
 * Why: 13.5c, the form open when someone turned "Simplificar dívidas" on or off and the pair owes nothing anymore:
 * "Ana simplificou as dívidas agora há pouco. Você não deve mais a Bruno na Viagem Floripa.", with what was owed struck
 * through and "Já paguei mesmo assim", since the Pix may already be gone.
 */
data class SimplifyChangedNotice(
    // Why: null for nobody known (no account, someone no longer in the group), whom each UI calls "Alguém".
    val changedByName: String?,
    val simplifiesDebts: Boolean,
    val groupName: String,
    // Why: what the pair owed as the form opened, which "Já paguei mesmo assim" records.
    val previousDebtMinor: Long,
)
