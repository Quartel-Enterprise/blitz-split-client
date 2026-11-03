package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: "Ler cupom fiscal" (7.1e to 7.1g) on a new bill, from the photo picked until it is saved or removed.
data class BillReceipt(
    val status: ReceiptStatus,
    // Why: "14 itens" beside "Lido do cupom", 0 for a receipt that lists none.
    val itemCount: Int,
    // Why: "Foto do cupom", attached to the bill once saved; false once nothing could be read from it.
    val hasPhoto: Boolean,
    // Why: why nothing was read, only while FAILED.
    val error: ReceiptError?,
)
