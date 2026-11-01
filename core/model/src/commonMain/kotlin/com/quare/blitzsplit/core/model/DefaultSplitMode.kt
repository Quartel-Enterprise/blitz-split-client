package com.quare.blitzsplit.core.model

/*
 * Why: the ways a group's "Divisão padrão" (6.7) splits its new bills, "Igual", "Partes", "Porcentagem" and "Pela
 * renda", in the order every UI lists them. Values in reais never become a default, since every bill has its own
 * total. The code is what Room and the API store, so it never changes even if an entry is renamed.
 */
enum class DefaultSplitMode(
    val code: String,
) {
    EQUAL("equal"),
    SHARES("shares"),
    PERCENT("percent"),
    INCOME("income"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): DefaultSplitMode? = entries.firstOrNull { it.code == code }
    }
}
