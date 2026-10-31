package com.quare.blitzsplit.feature.registerpayment.presentation.model

// Why: the title and the button follow who is recording the payment.
enum class PaymentKind {
    // Why: "Ana te pagou?", "Confirmar recebimento".
    RECEIVED,

    // Why: "Você pagou Bruno?", "Confirmar pagamento".
    PAID,

    // Why: "Diego pagou Bruno?", "Registrar pagamento", with "Você está registrando por Diego e Bruno."
    BETWEEN_OTHERS,
}
