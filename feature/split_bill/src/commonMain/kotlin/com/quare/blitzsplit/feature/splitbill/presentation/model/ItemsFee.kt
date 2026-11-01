package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: the two rows of "Serviço e couvert", each opening its own field.
enum class ItemsFee {
    // Why: "Serviço (10%)", a whole percentage of the items.
    SERVICE,

    // Why: "Couvert", per person at the table.
    COUVERT,
}
