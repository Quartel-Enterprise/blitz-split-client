package com.quare.blitzsplit.feature.registerpayment.presentation.model

// Why: what "Pagar Ana" or the form copied, as its event's target param says; the text itself is never sent.
enum class PayCopyTarget {
    // Why: "Copiar chave" of "Chave padrão de Ana".
    KEY,

    // Why: "Copiar código" of "Pix copia e cola", the code with the amount in it.
    PIX_CODE,

    // Why: the copy icon of one of "Outras chaves de Ana".
    OTHER_KEY,

    // Why: the copy icon of one of the receiver's Pix keys in the form, with Pix picked (beta feedback 3 item 26).
    FORM_PIX_KEY,
}
