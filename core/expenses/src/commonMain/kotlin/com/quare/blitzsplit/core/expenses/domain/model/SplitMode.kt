package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.DefaultSplitMode

/*
 * Why: how a bill splits ("Igual", "Valores", "Porcentagem", "Partes", "Itens", "By income"). The code is the one Room
 * and the API store, except for "Itens": a bill split by its items is stored as "amount" with its items beside the
 * shares, so a version that doesn't know items still reads each one's total; and "By income", which only Dividir
 * offers: the bill keeps the percentages the incomes give, stored as "percent", so it reads back as "%".
 */
enum class SplitMode(
    val code: String,
) {
    EQUAL("equal"),
    AMOUNT("amount"),
    PERCENT("percent"),
    SHARES("shares"),
    ITEMS("items"),
    INCOME("income"),
    ;

    // Why: the "Divisão padrão" a bill split this way makes (beta feedback 11); Valores and Itens are one bill's amounts.
    val defaultMode: DefaultSplitMode?
        get() = when (this) {
            EQUAL -> DefaultSplitMode.EQUAL
            SHARES -> DefaultSplitMode.SHARES
            PERCENT -> DefaultSplitMode.PERCENT
            INCOME -> DefaultSplitMode.INCOME
            AMOUNT, ITEMS -> null
        }

    // Why: the mode a bill is kept in, "%" for "By income", whose percentages are what the bill keeps.
    val savedMode: SplitMode
        get() = if (this == INCOME) PERCENT else this

    companion object {
        fun fromCodeOrNull(code: String): SplitMode? = entries.firstOrNull { it.code == code }

        // Why: the code Room and the API keep, "amount" for a bill split by its items, "percent" for one by income.
        fun storedCodeOf(mode: SplitMode): String = if (mode == ITEMS) AMOUNT.code else mode.savedMode.code

        /*
         * Why: how a group's "Divisão padrão" splits a bill; "Pela renda" splits by income, from the group's incomes,
         * and by the percentages it saved when those incomes no longer give them.
         */
        fun of(mode: DefaultSplitMode): SplitMode = when (mode) {
            DefaultSplitMode.EQUAL -> EQUAL
            DefaultSplitMode.SHARES -> SHARES
            DefaultSplitMode.PERCENT -> PERCENT
            DefaultSplitMode.INCOME -> INCOME
        }
    }
}
