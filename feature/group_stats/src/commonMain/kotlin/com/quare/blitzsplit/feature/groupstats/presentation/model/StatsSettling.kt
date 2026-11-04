package com.quare.blitzsplit.feature.groupstats.presentation.model

// Why: "Acertos": "Acerta mais rápido" and "Demora mais para acertar", always two different people.
data class StatsSettling(
    val fastest: StatsSettler,
    val slowest: StatsSettler,
)
