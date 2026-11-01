package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: one person in "Item" (7.6), with what each mode keeps for them.
data class ItemPersonRow(
    val person: BillPerson,
    // Why: Igual, the checkbox.
    val isIncluded: Boolean,
    // Why: Unidades and Partes, the stepper.
    val count: Long,
    // Why: Valor and %, the field as typed ("40,00", "67").
    val valueText: String,
)
