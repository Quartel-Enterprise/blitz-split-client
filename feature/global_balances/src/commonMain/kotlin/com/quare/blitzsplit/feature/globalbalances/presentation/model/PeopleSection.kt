package com.quare.blitzsplit.feature.globalbalances.presentation.model

// Why: "Você deve · 1" or "Te devem · 4", with what the section adds up to in each currency.
data class PeopleSection(
    val isYouOwe: Boolean,
    val rows: List<PersonRow>,
    val totals: List<CurrencyAmount>,
)
