package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: "Bruno ficou com 2 partes.", for whoever has more than one part of an item.
data class ItemPersonParts(
    val person: BillPerson,
    val parts: Long,
)
