package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: one person in "Dividir", with what each mode keeps for them.
data class SplitRow(
    val person: BillPerson,
    // Why: "Igual": the checkbox.
    val isIncluded: Boolean,
    // Why: "Valores" and "Porcentagem": the field as typed ("62,40", "25").
    val valueText: String,
    // Why: "Partes": the stepper.
    val parts: Long,
    // Why: what the split gives them, null while it doesn't add up; shown under the name or beside it.
    val shareMinor: Long?,
)
