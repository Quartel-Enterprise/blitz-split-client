package com.quare.blitzsplit.core.expenses.domain.model

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
    /*
     * Why: "usada em 3 contas": the group's bills with it or a subcategory of it; null while those bills aren't read
     * yet, so nothing says 0 before its time.
     */
    val usage: Int?,
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

    // Why: the preview's color: its parent's for a subcategory, the one picked in the form for a main category.
    val color: CategoryTint
        get() = parent?.color ?: form.color

    // Why: "Sem internet: a categoria fica salva no aparelho e sobe ao reconectar." (7.8d).
    val showsOfflineNotice: Boolean
        get() = !isOnline

    // Why: "Apagar categoria" waits for the bills it would move, so its question never counts them as none.
    val canDelete: Boolean
        get() = form.isEditing && usage != null && !form.isSaving

    // Why: "Apagar “Churrasco”?" says where its bills go: its parent, or Outros for a main category (7.8f).
    fun findDeletionOrNull(): CategoryDeletion? {
        if (!canDelete) return null
        val category = categories.findOrNull(form.categoryId) ?: return null
        return CategoryDeletion(
            category = category,
            billCount = usage ?: 0,
            movesTo = category.parentId?.let(categories::resolve) ?: categories.other,
            isDeleting = false,
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
