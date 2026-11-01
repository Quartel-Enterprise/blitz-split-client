package com.quare.blitzsplit.core.model

/*
 * Why: how "Ao longo do tempo" groups a period's spending: "Dia", "Semana", "Mês" or "Ano", in the order every UI
 * lists them. The code is what the device keeps, so it never changes even if an entry is renamed.
 */
enum class StatsGrain(
    val code: String,
) {
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    YEAR("year"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): StatsGrain? = entries.firstOrNull { it.code == code }
    }
}
