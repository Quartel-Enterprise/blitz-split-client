package com.quare.blitzsplit.feature.globalbalances.presentation.model

/*
 * Why: a group whose "Simplificar dívidas" made the person pay you instead of someone else, which Cobrar explains
 * (13.6b): "A Viagem Floripa simplificou as dívidas, então em vez de pagar Bruno você paga direto para mim; o seu
 * total não mudou."
 */
data class ChargeReroute(
    val groupName: String,
    val insteadOfNames: List<String>,
)
