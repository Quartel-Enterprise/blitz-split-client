package com.quare.blitzsplit.feature.splitbill.presentation.model

/*
 * Why: "Categoria" (7.7), open over Rachar conta while not null: what is typed in "Buscar categoria" and the main
 * category whose subcategories show (inline on a phone, beside the list when there is room).
 */
data class CategoryPicker(
    val query: String,
    val openMainId: String?,
)
