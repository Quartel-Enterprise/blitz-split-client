package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon

/*
 * Why: "Nova categoria", "Nova subcategoria" and "Editar categoria" (7.8), over the picker while not null. Its Salvar
 * changes the group's categories, and a new one becomes the bill's.
 */
data class CategoryForm(
    // Why: the group category being edited; null for a new one.
    val categoryId: String?,
    val name: String,
    // Why: "Dentro de": null for a main category.
    val parentId: String?,
    // Why: a main category's; a subcategory shows its parent's.
    val icon: CategoryIcon,
    // Why: "Criada por Ana", for one being edited.
    val createdByMemberId: String?,
    val isSaving: Boolean,
) {
    val isEditing: Boolean
        get() = categoryId != null

    val isSubcategory: Boolean
        get() = parentId != null
}
