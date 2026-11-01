package com.quare.blitzsplit.feature.categorymap.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category

// Why: a main category of the map (9.1), "Transporte · 7 termos", with its lines; a group's main says whose it is.
data class MapSection(
    val category: Category,
    val groupName: String?,
    val subcategoryCount: Int,
    val rows: List<MapRow>,
) {
    val termCount: Int
        get() = rows.sumOf { it.terms.size }
}
