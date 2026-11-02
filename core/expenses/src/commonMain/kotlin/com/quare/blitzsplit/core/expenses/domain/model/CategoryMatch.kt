package com.quare.blitzsplit.core.expenses.domain.model

// Why: the term of the map a title has, which Rachar conta marks under the description ("pela palavra “mercado”").
data class CategoryMatch(
    val term: String,
    val categoryId: String,
    // Why: where the term is in the title, so each UI can mark it.
    val start: Int,
    val end: Int,
)
