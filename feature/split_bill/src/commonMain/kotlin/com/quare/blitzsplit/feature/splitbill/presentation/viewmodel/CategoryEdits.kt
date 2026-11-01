package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMatcher
import com.quare.blitzsplit.core.expenses.domain.model.CategoryRules
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTerm
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryForm
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryPicker
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState

/*
 * Why: the map's terms in the description (7.1), only those whose category lives in this group. Until the category is
 * picked by hand, the bill follows the term that chooses first, or "Outros" with none (7.1m).
 */
internal fun SplitBillUiState.withCategoryMatched(terms: List<CategoryTerm>): SplitBillUiState {
    val matches = CategoryMatcher
        .matches(description, terms.filter { categories.findOrNull(it.categoryId) != null })
        .distinctBy { CategoryMatcher.keyOf(it.term) }
    return copy(
        categoryMatches = matches,
        categoryId = if (isCategoryPicked) categoryId else matches.firstOrNull()?.categoryId,
    )
}

// Why: the group's categories and how its bills use them, as a sync or a change here moves them.
internal fun SplitBillUiState.withCategories(
    tree: CategoryTree,
    expenses: List<Expense>,
): SplitBillUiState = copy(
    categories = tree,
    categoryUsage = expenses
        .filter { it.categoryId != null }
        .groupingBy { tree.resolve(it.categoryId).id }
        .eachCount(),
)

// Why: the picker opens on the bill's main category, with what it holds already marked.
internal fun SplitBillUiState.withCategoryPickerOpened(): SplitBillUiState = copy(
    categoryPicker = CategoryPicker(
        query = "",
        openMainId = categories.mainOf(category).id,
    ),
)

internal fun SplitBillUiState.withCategoryPicked(categoryId: String): SplitBillUiState {
    val picked = categories.findOrNull(categoryId) ?: return this
    return copy(
        categoryId = picked.id,
        isCategoryPicked = true,
        categoryPicker = null,
        categoryForm = null,
    )
}

internal fun SplitBillUiState.withCategoryMainToggled(mainId: String): SplitBillUiState {
    val picker = categoryPicker ?: return this
    val branch = categories.branchOf(mainId) ?: return this
    // Why: "Outros" has no subcategories nor takes any, so it is picked as it is.
    if (branch.category.isOther) return withCategoryPicked(mainId)
    return copy(categoryPicker = picker.copy(openMainId = mainId.takeIf { it != picker.openMainId }))
}

// Why: "Nova em Alimentação" starts inside it; a new main category starts with the first icon of the grid.
internal fun SplitBillUiState.withNewCategoryForm(
    parentId: String?,
    name: String,
): SplitBillUiState {
    val parent = parentId?.let { categories.findOrNull(it) }?.takeIf { it.isMain && !it.isOther }
    return copy(
        categoryForm = CategoryForm(
            categoryId = null,
            name = CategoryRules.toCapped(name.trim()),
            parentId = parent?.id,
            icon = parent?.icon ?: CategoryIcon.PAW,
            createdByMemberId = null,
            isSaving = false,
        ),
    )
}

// Why: only the group's own categories may be edited (7.8e).
internal fun SplitBillUiState.withEditCategoryForm(categoryId: String): SplitBillUiState {
    val category = categories.findOrNull(categoryId)?.takeIf { it.isGroupCategory } ?: return this
    return copy(
        categoryForm = CategoryForm(
            categoryId = category.id,
            name = category.name.orEmpty(),
            parentId = category.parentId,
            icon = category.icon,
            createdByMemberId = category.createdByMemberId,
            isSaving = false,
        ),
    )
}

internal fun SplitBillUiState.withCategoryParent(parentId: String?): SplitBillUiState {
    val form = categoryForm ?: return this
    if (isCategoryParentLocked) return this
    val parent = parentId?.let { id -> categoryParents.firstOrNull { it.id == id } }
    return copy(
        categoryForm = form.copy(
            parentId = parent?.id,
            icon = if (parent != null) parent.icon else form.icon,
        ),
    )
}
