package com.quare.blitzsplit.feature.groupstats.presentation.model

// Why: one day, week, month or year of "Ao longo do tempo", as a bar or a point of the line.
data class StatsBar(
    // Why: the first instant of its first and last days.
    val startMillis: Long,
    val endMillis: Long,
    val amountMinor: Long,
    // Why: a day bar of today, labeled "hoje".
    val isToday: Boolean,
    // Why: "Dia mais caro", "Semana mais cara"…: the first of the most expensive ones.
    val isPeak: Boolean,
    // Why: with many bars, only some carry a label under the chart ("Com 44 semanas, os rótulos mostram só alguns").
    val showsLabel: Boolean,
    // Why: "Na linha, um ponto em cada valor até 12; com mais, só o ponto do mais caro."
    val showsDot: Boolean,
)
