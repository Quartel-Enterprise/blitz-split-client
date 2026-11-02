@file:JsExport

package com.quare.blitzsplit.shared.bridge

import com.quare.blitzsplit.core.expenses.domain.model.MapTerm
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.presentation.bridge.ViewModelHolder
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiAction
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiState
import com.quare.blitzsplit.feature.categorymap.presentation.model.MapRow
import com.quare.blitzsplit.feature.categorymap.presentation.model.MapSection
import com.quare.blitzsplit.feature.categorymap.presentation.viewmodel.CategoryMapViewModel
import kotlinx.coroutines.flow.map
import org.koin.mp.KoinPlatform

class JsMapTerm(
    val term: String,
    val key: String,
    val categoryId: String,
    val isAppTerm: Boolean,
    val isPending: Boolean,
)

// Why: a line of a category's card: a subcategory, or "Só Transporte" (isOnlyMain), with its terms.
class JsMapRow(
    val category: JsCategory,
    val parent: JsCategory?,
    val isOnlyMain: Boolean,
    val groupName: String?,
    val terms: Array<JsMapTerm>,
)

class JsMapSection(
    val category: JsCategory,
    val groupName: String?,
    val termCount: Int,
    val subcategoryCount: Int,
    val rows: Array<JsMapRow>,
)

class JsMapSearchResult(
    val term: JsMapTerm,
    val category: JsCategory,
    val parent: JsCategory?,
    val matchStart: Int,
    val matchEnd: Int,
)

// Why: a category a term may send titles to, with its group's name for one created in a group.
class JsMapCategoryOption(
    val category: JsCategory,
    val parent: JsCategory?,
    val groupName: String?,
)

class JsTermForm(
    val isEditing: Boolean,
    val isAppTerm: Boolean,
    val text: String,
    val categoryId: String,
    val category: JsCategory?,
    val categoryParent: JsCategory?,
    val isSaving: Boolean,
    // Why: "too-short", "too-long" or "invalid-characters", null while the word is fine or empty.
    val error: String?,
    // Why: the term of another category the word already is, which turns Salvar into "Mover para X".
    val duplicateTerm: String?,
    val duplicateCategory: JsCategory?,
    val duplicateParent: JsCategory?,
    val canSave: Boolean,
)

class JsCategoryMapState(
    val isLoaded: Boolean,
    val query: String,
    val isSearching: Boolean,
    val sections: Array<JsMapSection>,
    val selectedCategoryId: String?,
    val results: Array<JsMapSearchResult>,
    val showsNoResult: Boolean,
    val isEmpty: Boolean,
    val isOnline: Boolean,
    val pendingCount: Int,
    val showsOfflineNotice: Boolean,
    val showsDeviceOnlyNotice: Boolean,
    val form: JsTermForm?,
    val options: Array<JsMapCategoryOption>,
    val removedTerm: String?,
    val isRestoreConfirmOpen: Boolean,
    val ownTerms: Array<String>,
)

class JsCategoryMapAction(
    // Why: "save-error".
    val type: String,
)

class CategoryMapPresenter {
    private val holder = ViewModelHolder(CategoryMapViewModel::class) { KoinPlatform.getKoin().get() }
    private val viewModel get() = holder.viewModel

    fun observeState(onChange: (JsCategoryMapState) -> Unit): JsWatch {
        val watch = FlowWatcher(viewModel.uiState.map(::toJsMapState)).observe(onChange)
        return JsWatch(watch::cancel)
    }

    fun observeActions(onAction: (JsCategoryMapAction) -> Unit): JsWatch {
        val actions = viewModel.uiAction.map { action ->
            when (action) {
                CategoryMapUiAction.ShowSaveError -> JsCategoryMapAction("save-error")
            }
        }
        val watch = FlowWatcher(actions).observe(onAction)
        return JsWatch(watch::cancel)
    }

    fun back() {
        viewModel.onEvent(CategoryMapUiEvent.OnBackClicked)
    }

    fun openProfile() {
        viewModel.onEvent(CategoryMapUiEvent.OnProfileClicked)
    }

    fun changeQuery(text: String) {
        viewModel.onEvent(CategoryMapUiEvent.OnQueryChanged(text))
    }

    // Why: what the search field shows right after a keystroke, before the next state arrives.
    fun getQuery(): String = viewModel.uiState.value.query

    fun selectSection(categoryId: String) {
        viewModel.onEvent(CategoryMapUiEvent.OnSectionSelected(categoryId))
    }

    fun addTerm(
        categoryId: String?,
        text: String,
    ) {
        viewModel.onEvent(CategoryMapUiEvent.OnAddTermClicked(categoryId, text))
    }

    fun openTerm(key: String) {
        viewModel.onEvent(CategoryMapUiEvent.OnTermClicked(key))
    }

    fun changeTermText(text: String) {
        viewModel.onEvent(CategoryMapUiEvent.OnTermTextChanged(text))
    }

    fun getTermText(): String = viewModel.uiState.value.termForm
        ?.text
        .orEmpty()

    fun pickTermCategory(categoryId: String) {
        viewModel.onEvent(CategoryMapUiEvent.OnTermCategoryPicked(categoryId))
    }

    fun saveTerm() {
        viewModel.onEvent(CategoryMapUiEvent.OnTermSaveClicked)
    }

    fun removeTerm() {
        viewModel.onEvent(CategoryMapUiEvent.OnTermRemoveClicked)
    }

    fun dismissTerm() {
        viewModel.onEvent(CategoryMapUiEvent.OnTermDismissed)
    }

    fun undoRemove() {
        viewModel.onEvent(CategoryMapUiEvent.OnUndoRemoveClicked)
    }

    fun dismissRemoved() {
        viewModel.onEvent(CategoryMapUiEvent.OnRemovedNoticeDismissed)
    }

    fun askToRestore() {
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreClicked)
    }

    fun confirmRestore() {
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreConfirmed)
    }

    fun dismissRestore() {
        viewModel.onEvent(CategoryMapUiEvent.OnRestoreDismissed)
    }

    fun dispose() {
        holder.clear()
    }
}

private fun toJsMapState(state: CategoryMapUiState): JsCategoryMapState {
    val form = state.termForm
    return JsCategoryMapState(
        isLoaded = state.isLoaded,
        query = state.query,
        isSearching = state.isSearching,
        sections = state.sections.map { toJsMapSection(it, state) }.toTypedArray(),
        selectedCategoryId = state.selectedSection?.category?.id,
        results = state.searchResults
            .map { result ->
                JsMapSearchResult(
                    term = toJsMapTerm(result.term),
                    category = toJsCategory(result.category),
                    parent = result.parent?.let(::toJsCategory),
                    matchStart = result.matchStart,
                    matchEnd = result.matchEnd,
                )
            }.toTypedArray(),
        showsNoResult = state.showsNoResult,
        isEmpty = state.isEmpty,
        isOnline = state.isOnline,
        pendingCount = state.map.pendingCount,
        showsOfflineNotice = state.showsOfflineNotice,
        showsDeviceOnlyNotice = state.showsDeviceOnlyNotice,
        form = form?.let {
            JsTermForm(
                isEditing = it.isEditing,
                isAppTerm = it.original?.isAppTerm == true,
                text = it.text,
                categoryId = it.categoryId,
                category = state.formCategory?.let(::toJsCategory),
                categoryParent = state.formCategoryParent?.let(::toJsCategory),
                isSaving = it.isSaving,
                error = state.termError
                    ?.name
                    ?.lowercase()
                    ?.replace('_', '-'),
                duplicateTerm = state.duplicateTerm?.term,
                duplicateCategory = state.duplicateCategory?.let(::toJsCategory),
                duplicateParent = state.duplicateCategory?.let { category -> parentOf(state.categories, category) },
                canSave = state.canSaveTerm,
            )
        },
        options = state.categoryOptions
            .map { option ->
                JsMapCategoryOption(
                    category = toJsCategory(option),
                    parent = parentOf(state.categories, option),
                    groupName = state.groupNameOf(option),
                )
            }.toTypedArray(),
        removedTerm = state.removedTerm?.term,
        isRestoreConfirmOpen = state.isRestoreConfirmOpen,
        ownTerms = state.ownTerms.map { it.term }.toTypedArray(),
    )
}

private fun toJsMapSection(
    section: MapSection,
    state: CategoryMapUiState,
): JsMapSection = JsMapSection(
    category = toJsCategory(section.category),
    groupName = section.groupName,
    termCount = section.termCount,
    subcategoryCount = section.subcategoryCount,
    rows = section.rows.map { toJsMapRow(it, state) }.toTypedArray(),
)

private fun toJsMapRow(
    row: MapRow,
    state: CategoryMapUiState,
): JsMapRow = JsMapRow(
    category = toJsCategory(row.category),
    parent = parentOf(state.categories, row.category),
    isOnlyMain = row.isOnlyMain,
    groupName = row.groupName,
    terms = row.terms.map(::toJsMapTerm).toTypedArray(),
)

private fun toJsMapTerm(term: MapTerm): JsMapTerm = JsMapTerm(
    term = term.term,
    key = term.key,
    categoryId = term.categoryId,
    isAppTerm = term.isAppTerm,
    isPending = term.isPending,
)
