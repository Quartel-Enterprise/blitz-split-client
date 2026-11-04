package com.quare.blitzsplit.feature.registerpayment.presentation.model

/*
 * Why: why a pair "Simplificar dívidas" made or grew is on the form (13.5): "Dívida simplificada: Diego paga a você o
 * que devia a Bruno." for a pair no bill made, "Dívida simplificada: inclui o que Ana devia a você." for one that grew.
 */
data class SimplifiedPairNote(
    val isNewPair: Boolean,
    // Why: whom the payer owed instead, by the bills, who now gets less from them.
    val insteadOf: List<NotePerson>,
)
