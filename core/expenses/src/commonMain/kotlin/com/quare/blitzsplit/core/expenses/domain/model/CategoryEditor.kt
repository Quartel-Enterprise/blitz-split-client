package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.AvatarColor

/*
 * Why: what "Nova categoria" (7.8) draws, the same from a bill's Categoria and from the map (flow 9): the form, the
 * categories of the group it goes in, which its name and "Dentro de" are checked against, and "Apagar “X”?" over it.
 */
data class CategoryEditor(
    val form: CategoryForm,
    // Why: the app's categories with those of the form's group alone, never another group's.
    val categories: CategoryTree,
    // Why: "Fica para todo o Viagem Floripa".
    val groupName: String,
    // Why: "Criada por Ana", for one being edited whose creator is still in the group.
    val creatorName: String?,
    // Why: "usada em 3 contas": the group's bills with it or a subcategory of it.
    val usage: Int,
    val deletion: CategoryDeletion?,
    val isOnline: Boolean,
) {
    val parent: Category?
        get() = form.parentId?.let(categories::findOrNull)

    val parents: List<Category>
        get() = form.findParentsIn(categories)

    val isParentLocked: Boolean
        get() = form.isParentLockedIn(categories)

    val isNameTaken: Boolean
        get() = form.isNameTakenIn(categories)

    val canSave: Boolean
        get() = form.canSaveIn(categories)

    // Why: the preview's color: its parent's, its own while edited, else the one a new main category gets next.
    val color: AvatarColor
        get() = parent?.color ?: categories.findOrNull(form.categoryId)?.color ?: categories.nextMainColor

    // Why: "Sem internet: a categoria fica salva no aparelho e sobe ao reconectar." (7.8d).
    val showsOfflineNotice: Boolean
        get() = !isOnline

    // Why: "Apagar “Churrasco”?" says where its bills go: its parent, or Outros for a main category (7.8f).
    fun withDeletionAsked(): CategoryEditor {
        val category = categories.findOrNull(form.categoryId) ?: return this
        val movesTo = category.parentId?.let(categories::resolve) ?: categories.other
        return copy(
            deletion = CategoryDeletion(
                category = category,
                billCount = usage,
                movesTo = movesTo,
                isDeleting = false,
            ),
        )
    }

    companion object {
        // Why: the bills with the category being edited, or a subcategory of it, by the category they read as.
        fun usageOf(
            categories: CategoryTree,
            categoryId: String?,
            usageByCategory: Map<String, Int>,
        ): Int {
            val id = categoryId ?: return 0
            val subcategories = categories
                .branchOf(id)
                ?.subcategories
                .orEmpty()
                .map { it.id }
            return (listOf(id) + subcategories).sumOf { usageByCategory[it] ?: 0 }
        }
    }
}
