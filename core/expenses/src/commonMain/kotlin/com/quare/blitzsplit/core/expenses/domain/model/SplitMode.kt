package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.DefaultSplitMode

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

        // Why: how a group's "Divisão padrão" splits a bill; "Pela renda" splits by the percentages it gives.
        fun of(mode: DefaultSplitMode): SplitMode = when (mode) {
            DefaultSplitMode.EQUAL -> EQUAL
            DefaultSplitMode.SHARES -> SHARES
            DefaultSplitMode.PERCENT, DefaultSplitMode.INCOME -> PERCENT
        }
    }
}
