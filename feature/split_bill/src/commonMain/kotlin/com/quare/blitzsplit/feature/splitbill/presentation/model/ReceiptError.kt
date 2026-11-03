package com.quare.blitzsplit.feature.splitbill.presentation.model

// Why: why "Ler cupom fiscal" read nothing (7.1g), which each UI words its own way.
enum class ReceiptError(
    // Why: "Tentar de novo" sends the same photo again, since the photo wasn't what failed.
    val canRetry: Boolean,
) {
    // Why: "Não deu para ler o cupom…" with "Tirar outra foto": no receipt nor total, or a photo that couldn't be opened.
    UNREADABLE(canRetry = false),

    // Why: "O leitor de cupom não respondeu. Não é a sua foto…" with "Tentar de novo".
    READER_UNAVAILABLE(canRetry = true),

    // Why: "Você já leu 30 cupons nas últimas 24 horas…", with no button.
    LIMIT_REACHED(canRetry = false),

    // Why: "Sem conexão. A foto não chegou ao leitor…" with "Tentar de novo".
    OFFLINE(canRetry = true),
}
