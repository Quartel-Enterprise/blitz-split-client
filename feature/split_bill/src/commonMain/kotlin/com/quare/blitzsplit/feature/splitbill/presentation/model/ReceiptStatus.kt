package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: where a receipt's reading is (7.1e to 7.1g).
enum class ReceiptStatus {
    // Why: the photo is on its way to the reader; Salvar waits for it.
    READING,

    // Why: "Lido do cupom": the amount, description, date and items came from it.
    READ,

    // Why: nothing was read, BillReceipt.error says why (7.1g).
    FAILED,
}
