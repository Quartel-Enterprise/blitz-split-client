package com.quare.blitzsplit.core.expenses.domain.model

// Why: a word or phrase of a bill's title that suggests its category (flow 9), "uber eats" → Delivery.
data class CategoryTerm(
    // Why: as written ("farmácia"), which the map shows and the title's match marks.
    val term: String,
    // Why: an app category's key or a group category's id, which counts only in that group's bills.
    val categoryId: String,
)
