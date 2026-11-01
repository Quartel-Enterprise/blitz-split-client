package com.quare.blitzsplit.feature.categorymap.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.MapTerm

// Why: one line of a category's card (9.1): a subcategory, or "Só Transporte" for the main one, with its terms.
data class MapRow(
    val category: Category,
    val isOnlyMain: Boolean,
    // Why: "só no Viagem Floripa" for a category created in a group, where its terms count only.
    val groupName: String?,
    val terms: List<MapTerm>,
)
