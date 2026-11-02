package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category

// Why: "Apagar “Churrasco”?" (7.8f): "As 3 contas com Churrasco passam para Alimentação."
data class CategoryDeletion(
    val category: Category,
    // Why: the group's bills it moves, its subcategories' included.
    val billCount: Int,
    // Why: where they go: its parent, or "Outros" for a main category.
    val movesTo: Category,
    val isDeleting: Boolean,
)
