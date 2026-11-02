package com.quare.blitzsplit.feature.categorymap.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.MapTerm

// Why: "uber eats" found by "uber" (9.1b), its matched part marked, with where it sends a title.
data class MapSearchResult(
    val term: MapTerm,
    val category: Category,
    val parent: Category?,
    // Why: the part of the term the search matched, empty when it can't be marked (the folded term differs in length).
    val matchStart: Int,
    val matchEnd: Int,
)
