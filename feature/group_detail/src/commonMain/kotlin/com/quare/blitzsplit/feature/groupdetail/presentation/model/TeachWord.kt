package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category

// Why: a word of the bill's title "Ensinar ao mapa" offers (9.3f): new in the map, or moving from where it is.
data class TeachWord(
    val term: String,
    val key: String,
    // Why: where the map sends the word now; null for a word new in the map.
    val currentCategory: Category?,
)
