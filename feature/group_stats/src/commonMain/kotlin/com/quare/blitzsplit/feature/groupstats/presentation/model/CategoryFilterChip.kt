package com.quare.blitzsplit.feature.groupstats.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category

// Why: a main category of "Expenses by category"'s chips; picking one adds its bills, its subcategories' included.
data class CategoryFilterChip(
    val category: Category,
    val isSelected: Boolean,
)
