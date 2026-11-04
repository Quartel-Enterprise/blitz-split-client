package com.quare.blitzsplit.feature.categorymap.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.CategoryForm
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteGroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveGroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.invoke
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiAction
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiState
import com.quare.blitzsplit.feature.categorymap.presentation.model.GroupBills
import com.quare.blitzsplit.feature.categorymap.presentation.model.TermForm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: creating, editing and deleting a group's category from the map (9.4), with the rules a bill's Categoria has
 * (7.8): it lives in one group and syncs to its members, offline too. A new one opens "Novo termo" in it next.
 */
internal class MapCategoryEditing(
    private val uiState: MutableStateFlow<CategoryMapUiState>,
    private val scope: CoroutineScope,
    private val saveGroupCategory: SaveGroupCategory,
    private val deleteGroupCategory: DeleteGroupCategory,
    private val emit: suspend (CategoryMapUiAction) -> Unit,
) {
    // Why: "usada em 3 contas" and "Apagar “X”?" count the bills of the form's group, as a sync moves them.
    fun observe(observeExpenses: ObserveExpenses) {
        uiState
            .map { it.categoryForm?.groupId }
            .distinctUntilChanged()
            .flatMapLatest { groupId ->
                groupId?.let { id -> observeExpenses(id).map { GroupBills(id, it) } } ?: flowOf(null)
            }.onEach { bills -> uiState.update { it.copy(categoryBills = bills) } }
            .launchIn(scope)
    }

    fun onEvent(event: CategoryMapUiEvent.Categories) {
        when (event) {
            is CategoryMapUiEvent.OnNewCategoryClicked -> openNew(event.parentId)

            is CategoryMapUiEvent.OnEditCategoryClicked -> openEdit(event.categoryId)

            is CategoryMapUiEvent.OnCategoryNameChanged -> changeForm { _, form -> form.withName(event.text) }

            is CategoryMapUiEvent.OnCategoryGroupPicked -> changeForm { state, form ->
                val group = state.groups.firstOrNull { it.id == event.groupId }
                if (form.isEditing || group == null) {
                    form
                } else {
                    // Why: "Dentro de" keeps the app's main category, but another group's own one isn't in this group.
                    form.copy(groupId = group.id).withParent(state.categories.ofGroup(group.id), form.parentId)
                }
            }

            is CategoryMapUiEvent.OnCategoryParentPicked -> changeForm { state, form ->
                form.withParent(state.categories.ofGroup(form.groupId), event.parentId)
            }

            is CategoryMapUiEvent.OnCategoryIconPicked -> changeForm { _, form -> form.withIcon(event.icon) }

            CategoryMapUiEvent.OnCategoryFormSaveClicked -> save()

            // Why: "Apagar “X”?" goes with its form, so it never opens over the next category's.
            CategoryMapUiEvent.OnCategoryFormDismissed -> uiState.update {
                if (it.categoryForm?.isSaving == true || it.categoryDeletion?.isDeleting == true) {
                    it
                } else {
                    it.copy(
                        categoryForm = null,
                        categoryDeletion = null,
                    )
                }
            }

            CategoryMapUiEvent.OnCategoryDeleteClicked -> uiState.update { state ->
                state.copy(categoryDeletion = state.categoryEditor?.findDeletionOrNull())
            }

            CategoryMapUiEvent.OnCategoryDeleteConfirmed -> delete()

            CategoryMapUiEvent.OnCategoryDeleteDismissed -> uiState.update {
                if (it.categoryDeletion?.isDeleting == true) it else it.copy(categoryDeletion = null)
            }
        }
    }

    private fun changeForm(transform: (CategoryMapUiState, CategoryForm) -> CategoryForm) {
        uiState.update { state ->
            // Why: what is saving is the form as Salvar found it, so nothing typed or picked meanwhile is lost unseen.
            val form = state.categoryForm?.takeIf { !it.isSaving } ?: return@update state
            state.copy(categoryForm = transform(state, form))
        }
    }

    /*
     * Why: a subcategory goes in its main category's group; a main category, or one under the app's, in the group of
     * the category picked beside the list, else the one with the latest activity.
     */
    private fun openNew(parentId: String?) {
        uiState.update { state ->
            if (!state.canCreateCategory) return@update state
            val selected = state.selectedSection?.category?.takeIf { state.sections.size > 1 && !state.isSearching }
            val groupId = parentId?.let(state.categories::groupIdOf)
                ?: selected?.let { state.categories.groupIdOf(it.id) }
                ?: state.groups.first().id
            state.copy(
                categoryForm = CategoryForm.create(
                    categories = state.categories.ofGroup(groupId),
                    groupId = groupId,
                    parentId = parentId,
                    name = "",
                ),
                categoryDeletion = null,
                termForm = null,
            )
        }
    }

    private fun openEdit(categoryId: String) {
        uiState.update { state ->
            val groupId = state.categories.groupIdOf(categoryId) ?: return@update state
            val form = CategoryForm.editingOrNull(state.categories, groupId, categoryId) ?: return@update state
            state.copy(
                categoryForm = form,
                categoryDeletion = null,
                termForm = null,
            )
        }
    }

    // Why: a new category opens "Novo termo" in it next, its section picked; an edited one just closes.
    private fun save() {
        val state = uiState.value
        val form = state.categoryForm ?: return
        if (state.categoryEditor?.canSave != true) return
        uiState.update { it.copy(categoryForm = form.copy(isSaving = true)) }
        val meId = state.groups.firstOrNull { it.id == form.groupId }?.meId
        scope.launch {
            saveGroupCategory(form, meId)
                .onSuccess { id ->
                    uiState.update {
                        if (form.isEditing) {
                            it.copy(categoryForm = null)
                        } else {
                            it.copy(
                                categoryForm = null,
                                selectedCategoryId = form.parentId ?: id,
                                query = "",
                                termForm = TermForm(
                                    original = null,
                                    text = "",
                                    categoryId = id,
                                    isPickingCategory = false,
                                    isSaving = false,
                                ),
                            )
                        }
                    }
                }.onFailure {
                    uiState.update { it.copy(categoryForm = it.categoryForm?.copy(isSaving = false)) }
                    emit(CategoryMapUiAction.ShowCategorySaveError)
                }
        }
    }

    // Why: its terms read as where its bills go, its parent or Outros, as a deleted category's do (9.1).
    private fun delete() {
        val deletion = uiState.value.categoryDeletion?.takeIf { !it.isDeleting } ?: return
        uiState.update { it.copy(categoryDeletion = deletion.copy(isDeleting = true)) }
        scope.launch {
            deleteGroupCategory(deletion.category.id)
                .onSuccess {
                    uiState.update { state ->
                        state.copy(
                            categoryDeletion = null,
                            categoryForm = null,
                            selectedCategoryId = state.selectedCategoryId.takeIf { it != deletion.category.id },
                        )
                    }
                }.onFailure {
                    uiState.update { it.copy(categoryDeletion = null) }
                    emit(CategoryMapUiAction.ShowCategoryDeleteError)
                }
        }
    }
}
