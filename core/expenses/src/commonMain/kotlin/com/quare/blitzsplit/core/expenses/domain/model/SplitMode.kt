package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.DefaultSplitMode

/*
 * Why: how a bill splits ("Igual", "Valores", "Porcentagem", "Partes", "Itens"). The code is the one Room and the API
 * store, except for "Itens": a bill split by its items is stored as "amount" with its items beside the shares, so a
 * version that doesn't know items still reads each one's total.
 */
enum class SplitMode(
    val code: String,
) {
    EQUAL("equal"),
    AMOUNT("amount"),
    PERCENT("percent"),
    SHARES("shares"),
    ITEMS("items"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): SplitMode? = entries.firstOrNull { it.code == code }

        // Why: the code Room and the API keep, "amount" for a bill split by its items.
        fun storedCodeOf(mode: SplitMode): String = if (mode == ITEMS) AMOUNT.code else mode.code

        // Why: how a group's "Divisão padrão" splits a bill; "Pela renda" splits by the percentages it gives.
        fun of(mode: DefaultSplitMode): SplitMode = when (mode) {
            DefaultSplitMode.EQUAL -> EQUAL
            DefaultSplitMode.SHARES -> SHARES
            DefaultSplitMode.PERCENT, DefaultSplitMode.INCOME -> PERCENT
        }
    }
}
