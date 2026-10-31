package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: one person in "Várias pessoas pagaram".
data class PayerAmountRow(
    val memberId: String,
    // Why: null for someone who left the group and paid part of the bill being edited, whom each UI calls "Ex-membro".
    val person: BillPerson?,
    // Why: the field as typed, "200,00".
    val amountText: String,
)
