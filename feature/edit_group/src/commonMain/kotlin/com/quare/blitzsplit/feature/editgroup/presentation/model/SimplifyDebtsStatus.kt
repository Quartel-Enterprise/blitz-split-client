package com.quare.blitzsplit.feature.editgroup.presentation.model

// Why: the line under "Simplificar dívidas" in Editar grupo (13.1), which each UI words.
enum class SimplifyDebtsStatus {
    // Why: "Junta as dívidas do grupo para dar menos pagamentos. Ninguém paga nem recebe a mais."
    OFF,

    // Why: "Ligado · 5 pagamentos em vez de 7", or "Ligado · já no mínimo de pagamentos".
    ON,

    // Why: "Ligado neste aparelho · aguardando envio" (or "Desligado…"), with the orange dot (13.1d).
    PENDING,

    // Why: "Com 2 pessoas já é um pagamento só. Fica disponível quando entrar a terceira pessoa." (13.1c)
    NEEDS_THIRD_PERSON,
}
