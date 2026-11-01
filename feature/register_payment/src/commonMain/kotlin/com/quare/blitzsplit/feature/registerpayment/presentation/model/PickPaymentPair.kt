package com.quare.blitzsplit.feature.registerpayment.presentation.model

// Why: who owes whom how much; a person no longer in the group is null, "Ex-membro".
data class PickPaymentPair(
    val from: PaymentPerson?,
    val to: PaymentPerson?,
    val fromMemberId: String,
    val toMemberId: String,
    val amountMinor: Long,
)
