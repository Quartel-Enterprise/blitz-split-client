package com.quare.blitzsplit.feature.groupstats.presentation.model

// Why: the reasons under the chart for the grains the period can't show, in the order the UIs write them.
enum class StatsGrainNote {
    // Why: "Dia vale para períodos de até 3 meses."
    DAY_NEEDS_SHORT_PERIOD,

    // Why: "Semana, Mês e Ano aparecem em períodos mais longos que uma semana."
    WEEK_MONTH_AND_YEAR_NEED_LONGER_PERIOD,

    // Why: "Mês e Ano aparecem em períodos mais longos que um mês."
    MONTH_AND_YEAR_NEED_LONGER_PERIOD,

    // Why: "Ano aparece em períodos mais longos que um ano."
    YEAR_NEEDS_LONGER_PERIOD,

    // Why: "Para comparar anos, escolha Ano: o período vira Todo o período."
    YEAR_SWITCHES_PERIOD,
}
