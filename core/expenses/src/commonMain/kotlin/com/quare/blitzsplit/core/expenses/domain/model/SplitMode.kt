package com.quare.blitzsplit.core.expenses.domain.model

// Why: how a bill splits ("Igual", "Valores", "Porcentagem", "Partes"). The code is the one Room and the API store.
enum class SplitMode(
    val code: String,
) {
    EQUAL("equal"),
    AMOUNT("amount"),
    PERCENT("percent"),
    SHARES("shares"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): SplitMode? = entries.firstOrNull { it.code == code }
    }
}
