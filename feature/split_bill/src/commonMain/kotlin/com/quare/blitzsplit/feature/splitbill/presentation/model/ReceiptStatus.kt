package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: where a receipt's reading is (7.1e to 7.1g).
enum class ReceiptStatus {
    // Why: the photo is on its way to the reader; Salvar waits for it.
    READING,

    // Why: "Lido do cupom": the amount, description, date and items came from it.
    READ,

    // Why: "Não deu para ler o cupom. Tente de novo com mais luz…", with "Tirar outra foto" (7.1g).
    UNREADABLE,
}
