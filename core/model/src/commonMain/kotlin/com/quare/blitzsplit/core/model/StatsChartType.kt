package com.quare.blitzsplit.core.model

// Why: "Barras" or "Linha" in "Ao longo do tempo". The code is what the device keeps.
enum class StatsChartType(
    val code: String,
) {
    BARS("bars"),
    LINE("line"),
    ;

    companion object {
        fun fromCodeOrNull(code: String): StatsChartType? = entries.firstOrNull { it.code == code }
    }
}
