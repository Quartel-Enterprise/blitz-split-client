package com.quare.blitzsplit.core.expenses.domain.model

// Why: a main category with its subcategories, the app's first and then the group's, as "Todas as categorias" lists them.
data class CategoryBranch(
    val category: Category,
    val subcategories: List<Category>,
)
