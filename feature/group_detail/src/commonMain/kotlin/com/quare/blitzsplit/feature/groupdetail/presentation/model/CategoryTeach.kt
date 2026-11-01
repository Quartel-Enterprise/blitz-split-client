package com.quare.blitzsplit.feature.groupdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Category

/*
 * Why: "Conta salva em Lazer › Esportes, e não em Casa › Aluguel, que o mapa sugeriu. Ensinar ao mapa?" (9.3e) with
 * Agora não and Ensinar, then "Ensinar ao mapa" (9.3f): which word of the title picks the category from now on.
 */
data class CategoryTeach(
    val expenseId: String,
    val title: String,
    val category: Category,
    val categoryParent: Category?,
    // Why: what the map picked from the title; null when no word of it did (9.3c).
    val mapCategory: Category?,
    val mapCategoryParent: Category?,
    val words: List<TeachWord>,
    val selectedKey: String?,
    val isPickingWord: Boolean,
    val isSaving: Boolean,
) {
    val selectedWord: TeachWord?
        get() = words.firstOrNull { it.key == selectedKey }
}
