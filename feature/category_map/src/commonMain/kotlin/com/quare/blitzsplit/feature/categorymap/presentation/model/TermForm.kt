package com.quare.blitzsplit.feature.categorymap.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.MapTerm

// Why: "Novo termo" (original null) or "Editar termo" (9.2), what is typed and the category it sends titles to.
data class TermForm(
    val original: MapTerm?,
    val text: String,
    val categoryId: String,
    // Why: the categories open under "Categoria" to pick another one.
    val isPickingCategory: Boolean,
    val isSaving: Boolean,
) {
    val isEditing: Boolean
        get() = original != null
}
