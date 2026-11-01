package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: "Conta salva em Lazer › Esportes, e não em Casa › Aluguel, que o mapa sugeriu. Ensinar ao mapa?" (9.3e), which
 * Despesas do grupo asks once the bill whose category was picked by hand is saved. Only while the app runs.
 */
data class CategoryTeachOffer(
    val expenseId: String,
    val groupId: String,
    val title: String,
    val categoryId: String,
    // Why: what the map would have picked; null when no term of the title matched (9.3c).
    val mapCategoryId: String?,
)
