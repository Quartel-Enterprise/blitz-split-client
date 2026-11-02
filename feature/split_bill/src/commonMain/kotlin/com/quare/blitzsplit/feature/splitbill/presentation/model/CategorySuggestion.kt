package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category

// Why: one of "Sugeridas" (7.7), with why it is there ("Pela descrição", "Mais usada no grupo", "Do grupo").
data class CategorySuggestion(
    val category: Category,
    val reason: CategorySuggestionReason,
    val isSelected: Boolean,
)
