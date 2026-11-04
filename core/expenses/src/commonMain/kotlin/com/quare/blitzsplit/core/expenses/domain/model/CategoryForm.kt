package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: "Nova categoria", "Nova subcategoria" and "Editar categoria" (7.8), from a bill's Categoria or from the map (flow
 * 9). Its Salvar changes its group's categories.
 */
data class CategoryForm(
    // Why: the group category being edited; null for a new one.
    val categoryId: String?,
    // Why: the group it goes in: the bill's in Rachar conta, the one picked in the map.
    val groupId: String,
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

    fun withName(text: String): CategoryForm = copy(name = CategoryRules.toCapped(text))

    // Why: a subcategory takes its parent's icon, so it has none to pick.
    fun withIcon(icon: CategoryIcon): CategoryForm = if (isSubcategory) this else copy(icon = icon)

    // Why: "Dentro de" lists only the group's main categories, and one with subcategories stays a main one.
    fun withParent(
        categories: CategoryTree,
        parentId: String?,
    ): CategoryForm {
        if (isParentLockedIn(categories)) return this
        val parent = parentId?.let { id -> findParentsIn(categories).firstOrNull { it.id == id } }
        return copy(
            parentId = parent?.id,
            icon = parent?.icon ?: icon,
        )
    }

    // Why: "Dentro de": "Nenhuma: categoria principal" and every main category but this one and Outros.
    fun findParentsIn(categories: CategoryTree): List<Category> =
        categories.branches.map { it.category }.filter { it.id != categoryId && !it.isOther }

    // Why: a category that has subcategories stays a main one, as there are only two levels.
    fun isParentLockedIn(categories: CategoryTree): Boolean = categoryId?.let(categories::hasSubcategories) == true

    // Why: "Já existe Churrasco em Alimentação. Escolha outro nome." (7.8c), among the app's names and the group's.
    fun isNameTakenIn(categories: CategoryTree): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        return categories.hasGroupName(parentId, trimmed, categoryId) ||
            categories.all.any { it.app != null && it.parentId == parentId && CategoryNames.isNamed(it, trimmed) }
    }

    fun canSaveIn(categories: CategoryTree): Boolean =
        CategoryRules.isNameValid(name) && !isNameTakenIn(categories) && !isSaving

    companion object {
        // Why: "Nova em Alimentação" starts inside it; a new main category starts with the first icon of the grid.
        fun create(
            categories: CategoryTree,
            groupId: String,
            parentId: String?,
            name: String,
        ): CategoryForm {
            val parent = parentId?.let { categories.findOrNull(it) }?.takeIf { it.isMain && !it.isOther }
            return CategoryForm(
                categoryId = null,
                groupId = groupId,
                name = CategoryRules.toCapped(name.trim()),
                parentId = parent?.id,
                icon = parent?.icon ?: CategoryIcon.PAW,
                createdByMemberId = null,
                isSaving = false,
            )
        }

        // Why: only the group's own categories may be edited (7.8e).
        fun editingOrNull(
            categories: CategoryTree,
            groupId: String,
            categoryId: String,
        ): CategoryForm? {
            val category = categories.findOrNull(categoryId)?.takeIf { it.isGroupCategory } ?: return null
            return CategoryForm(
                categoryId = category.id,
                groupId = groupId,
                name = category.name.orEmpty(),
                parentId = category.parentId,
                icon = category.icon,
                createdByMemberId = category.createdByMemberId,
                isSaving = false,
            )
        }
    }
}
