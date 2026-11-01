package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: how one item of "Itens da conta" splits among who consumed it (7.6): Igual, Unidades (2 de 6 cervejas),
 * Partes, Valor or %. The code is the one the bill's items keep in Room and on the server.
 */
enum class ItemSplitMode(
    val code: String,
) {
    EQUAL("equal"),
    UNITS("units"),
    SHARES("shares"),
    AMOUNT("amount"),
    PERCENT("percent"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): ItemSplitMode? = entries.firstOrNull { it.code == code }
    }
}
