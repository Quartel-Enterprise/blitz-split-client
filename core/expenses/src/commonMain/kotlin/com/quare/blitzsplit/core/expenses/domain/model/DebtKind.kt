package com.quare.blitzsplit.core.expenses.domain.model

enum class DebtKind {
    // Why: "Você deve a Bruno · −R$ 118,00", with "Acertar".
    YOU_OWE,

    // Why: "Ana te deve · +R$ 162,30", with "Recebi".
    OWES_YOU,

    // Why: "Diego deve a Bruno · R$ 64,00", with "Já pagou": anyone may record it.
    BETWEEN_OTHERS,
}
