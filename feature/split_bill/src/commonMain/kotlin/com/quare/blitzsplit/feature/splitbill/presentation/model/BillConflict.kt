package com.quare.blitzsplit.feature.splitbill.presentation.model

/*
 * Why: "Ana mudou esta conta há 2 minutos. Vale a última edição salva: se você salvar agora, a sua edição substitui
 * essa mudança." Someone saved the bill while it was being edited here.
 */
data class BillConflict(
    // Why: null when the account that saved it is no member of the group, which each UI calls "Alguém".
    val changerName: String?,
    // Why: this device's user, from another device ("Você mudou esta conta em outro aparelho").
    val changerIsMe: Boolean,
    // Why: each UI says how long ago, from the clock of the device that saved it.
    val changedAt: Long,
)
