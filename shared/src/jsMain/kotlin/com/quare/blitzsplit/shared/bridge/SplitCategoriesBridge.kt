@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.CategoryColor
import com.quare.blitzsplit.core.expenses.domain.model.CategoryIcon
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryHint
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import com.quare.blitzsplit.feature.splitbill.presentation.viewmodel.SplitBillViewModel

// Why: a word of the map in the description, where it is in it (start and end, as JavaScript slices).
class JsCategoryMatch(
    val term: String,
    val categoryId: String,
    val start: Int,
    val end: Int,
)

// Why: one of "Sugeridas"; reason is "description", "receipt", "most-used", "group" or "used".
class JsCategorySuggestion(
    val category: JsCategory,
    val reason: String,
    val isSelected: Boolean,
)

/*
 * Why: the bill's category and "Categoria" (7.7, 7.8). hint is "by-term", "by-longest-term", "by-receipt", "no-term",
 * "changed" or "picked"; the picker's fields are only meaningful while isPickerOpen.
 */
class JsBillCategories(
    val category: JsCategory,
    val parent: JsCategory?,
    val mapCategory: JsCategory?,
    val mapParent: JsCategory?,
    val hint: String,
    val matches: Array<JsCategoryMatch>,
    val isPickerOpen: Boolean,
    val query: String,
    val openMainId: String?,
    val branches: Array<JsCategoryBranch>,
    val suggestions: Array<JsCategorySuggestion>,
    val mostUsed: JsCategory?,
    val isSearching: Boolean,
    val results: Array<JsCategory>,
    val showsNoneFound: Boolean,
    val form: JsCategoryForm?,
    val deletion: JsCategoryDeletion?,
    // Why: the exception of the bill's category, which Dividir's notice and "Divisão" name.
    val defaultSplitException: JsCategory?,
    // Why: the exception the bill's split replaces once saved, which "Divisão" names (beta feedback 11).
    val splitAsDefaultException: JsCategory?,
    // Why: the exception "Usar como padrão do grupo" replaces, which its label names (beta feedback 11).
    val splitEditorDefaultException: JsCategory?,
    // Why: every CategoryIcon's code, in the order "Nova categoria" offers them.
    val icons: Array<String>,
)

internal fun toJsBillCategories(state: SplitBillUiState): JsBillCategories {
    val tree = state.categories
    val mapCategory = state.mapCategory
    return JsBillCategories(
        category = toJsCategory(state.category),
        parent = parentOf(tree, state.category),
        mapCategory = mapCategory?.let(::toJsCategory),
        mapParent = mapCategory?.let { parentOf(tree, it) },
        hint = when (state.categoryHint) {
            CategoryHint.BY_TERM -> "by-term"
            CategoryHint.BY_LONGEST_TERM -> "by-longest-term"
            CategoryHint.BY_RECEIPT -> "by-receipt"
            CategoryHint.NO_TERM -> "no-term"
            CategoryHint.CHANGED -> "changed"
            CategoryHint.PICKED -> "picked"
        },
        matches = state.categoryMatches
            .map {
                JsCategoryMatch(
                    term = it.term,
                    categoryId = it.categoryId,
                    start = it.start,
                    end = it.end,
                )
            }.toTypedArray(),
        isPickerOpen = state.categoryPicker != null,
        query = state.categoryPicker?.query.orEmpty(),
        openMainId = state.categoryPicker?.openMainId,
        branches = toJsBranches(tree),
        suggestions = state.categorySuggestions
            .map {
                JsCategorySuggestion(
                    category = toJsCategory(it.category),
                    reason = it.reason.name
                        .lowercase()
                        .replace('_', '-'),
                    isSelected = it.isSelected,
                )
            }.toTypedArray(),
        mostUsed = state.mostUsedCategory?.let(::toJsCategory),
        isSearching = state.isSearchingCategories,
        results = state.categoryResults.map(::toJsCategory).toTypedArray(),
        showsNoneFound = state.showsNoCategoryFound,
        form = state.categoryEditor?.let(::toJsCategoryForm),
        deletion = state.categoryDeletion?.let(::toJsCategoryDeletion),
        defaultSplitException = state.defaultSplitException?.let(::toJsCategory),
        splitAsDefaultException = state.splitAsDefaultException?.let(::toJsCategory),
        splitEditorDefaultException = state.splitEditorDefaultException?.let(::toJsCategory),
        icons = CategoryIcon.entries.map { it.code }.toTypedArray(),
    )
}

// Why: "Categoria" and "Nova categoria", on the bill's own ViewModel.
class SplitCategoriesPresenter internal constructor(
    private val viewModel: () -> SplitBillViewModel,
) {
    fun open() {
        send(SplitBillUiEvent.OnCategoryClicked)
    }

    // Why: the "Mapa de categorias" card (9.0), which opens the map over the bill.
    fun openMap() {
        send(SplitBillUiEvent.OnCategoryMapClicked)
    }

    fun changeQuery(text: String) {
        send(SplitBillUiEvent.OnCategoryQueryChanged(text))
    }

    fun getQuery(): String = viewModel()
        .uiState.value.categoryPicker
        ?.query
        .orEmpty()

    fun toggleMain(categoryId: String) {
        send(SplitBillUiEvent.OnCategoryMainToggled(categoryId))
    }

    fun pick(categoryId: String) {
        send(SplitBillUiEvent.OnCategoryPicked(categoryId))
    }

    fun dismiss() {
        send(SplitBillUiEvent.OnCategoryDismissed)
    }

    // Why: parentId null for a main category; name what the search had, or empty.
    fun create(
        parentId: String?,
        name: String,
    ) {
        send(SplitBillUiEvent.OnNewCategoryClicked(parentId, name))
    }

    fun edit(categoryId: String) {
        send(SplitBillUiEvent.OnEditCategoryClicked(categoryId))
    }

    fun changeName(text: String) {
        send(SplitBillUiEvent.OnCategoryNameChanged(text))
    }

    fun getName(): String = viewModel()
        .uiState.value.categoryForm
        ?.name
        .orEmpty()

    fun pickParent(parentId: String?) {
        send(SplitBillUiEvent.OnCategoryParentPicked(parentId))
    }

    fun pickIcon(code: String) {
        val icon = CategoryIcon.fromCodeOrNull(code) ?: return
        send(SplitBillUiEvent.OnCategoryIconPicked(icon))
    }

    fun pickColor(code: String) {
        val color = CategoryColor.fromCodeOrNull(code) ?: return
        send(SplitBillUiEvent.OnCategoryColorPicked(color))
    }

    fun save() {
        send(SplitBillUiEvent.OnCategoryFormSaveClicked)
    }

    fun dismissForm() {
        send(SplitBillUiEvent.OnCategoryFormDismissed)
    }

    fun askToDelete() {
        send(SplitBillUiEvent.OnCategoryDeleteClicked)
    }

    fun confirmDelete() {
        send(SplitBillUiEvent.OnCategoryDeleteConfirmed)
    }

    fun dismissDelete() {
        send(SplitBillUiEvent.OnCategoryDeleteDismissed)
    }

    private fun send(event: SplitBillUiEvent) {
        viewModel().onEvent(event)
    }
}
