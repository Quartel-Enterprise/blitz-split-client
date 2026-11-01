package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.domain.model.CategoryRules
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteGroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveGroupCategory
import com.quare.blitzsplit.feature.splitbill.presentation.model.CategoryDeletion
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: "Categoria" (7.7) and the group's own categories (7.8): picking one for the bill, and creating, editing and
 * deleting the group's, offline too (7.8d). The bill's split follows its category's exception, so every change of the
 * bill's category tells the ViewModel.
 */
internal class CategoryEditing(
    private val uiState: MutableStateFlow<SplitBillUiState>,
    private val scope: CoroutineScope,
    private val saveGroupCategory: SaveGroupCategory,
    private val deleteGroupCategory: DeleteGroupCategory,
    private val emit: suspend (SplitBillUiAction) -> Unit,
    private val onCategoryChanged: () -> Unit,
) {
    fun onEvent(event: SplitBillUiEvent.Categories) {
        when (event) {
            SplitBillUiEvent.OnCategoryClicked -> uiState.update {
                if (it.isLoaded) it.withCategoryPickerOpened() else it
            }

            is SplitBillUiEvent.OnCategoryQueryChanged -> uiState.update { state ->
                state.copy(categoryPicker = state.categoryPicker?.copy(query = event.text.take(MAX_QUERY_LENGTH)))
            }

            is SplitBillUiEvent.OnCategoryMainToggled -> change { it.withCategoryMainToggled(event.categoryId) }

            is SplitBillUiEvent.OnCategoryPicked -> change { it.withCategoryPicked(event.categoryId) }

            SplitBillUiEvent.OnCategoryDismissed -> uiState.update {
                it.copy(
                    categoryPicker = null,
                    categoryForm = null,
                )
            }

            is SplitBillUiEvent.OnNewCategoryClicked -> uiState.update { it.withNewCategoryForm(event.parentId, event.name) }

            is SplitBillUiEvent.OnEditCategoryClicked -> uiState.update { it.withEditCategoryForm(event.categoryId) }

            is SplitBillUiEvent.OnCategoryNameChanged -> uiState.update { state ->
                state.copy(categoryForm = state.categoryForm?.copy(name = CategoryRules.toCapped(event.text)))
            }

            is SplitBillUiEvent.OnCategoryParentPicked -> uiState.update { it.withCategoryParent(event.parentId) }

            is SplitBillUiEvent.OnCategoryIconPicked -> uiState.update { state ->
                val form = state.categoryForm?.takeIf { !it.isSubcategory } ?: return@update state
                state.copy(categoryForm = form.copy(icon = event.icon))
            }

            SplitBillUiEvent.OnCategoryFormSaveClicked -> save()

            SplitBillUiEvent.OnCategoryFormDismissed -> uiState.update {
                if (it.categoryForm?.isSaving == true) it else it.copy(categoryForm = null)
            }

            SplitBillUiEvent.OnCategoryDeleteClicked -> askToDelete()

            SplitBillUiEvent.OnCategoryDeleteConfirmed -> delete()

            SplitBillUiEvent.OnCategoryDeleteDismissed -> uiState.update {
                if (it.categoryDeletion?.isDeleting == true) it else it.copy(categoryDeletion = null)
            }
        }
    }

    private fun change(transform: (SplitBillUiState) -> SplitBillUiState) {
        val before = uiState.value.category.id
        uiState.update(transform)
        if (uiState.value.category.id != before) onCategoryChanged()
    }

    // Why: a new category becomes the bill's at once; an edited one goes back to the picker.
    private fun save() {
        val state = uiState.value
        val form = state.categoryForm ?: return
        if (!state.canSaveCategory) return
        uiState.update { it.copy(categoryForm = form.copy(isSaving = true)) }
        val meId = state.people.firstOrNull { it.isMe }?.id
        scope.launch {
            saveGroupCategory(
                categoryId = form.categoryId,
                groupId = state.groupId,
                name = form.name,
                parentId = form.parentId,
                icon = form.icon,
                createdByMemberId = form.createdByMemberId ?: meId,
            ).onSuccess { id ->
                if (form.isEditing) {
                    uiState.update { it.copy(categoryForm = null) }
                } else {
                    // Why: the new category reaches the tree a moment later, through Room.
                    uiState.update {
                        it.copy(
                            categoryId = id,
                            isCategoryPicked = true,
                            categoryForm = null,
                            categoryPicker = null,
                        )
                    }
                    onCategoryChanged()
                }
            }.onFailure {
                uiState.update { it.copy(categoryForm = it.categoryForm?.copy(isSaving = false)) }
                emit(SplitBillUiAction.ShowCategorySaveError)
            }
        }
    }

    private fun askToDelete() {
        val state = uiState.value
        val id = state.categoryForm?.categoryId ?: return
        val category = state.categories.findOrNull(id) ?: return
        val movesTo = category.parentId?.let { state.categories.resolve(it) } ?: state.categories.other
        uiState.update {
            it.copy(
                categoryDeletion = CategoryDeletion(
                    category = category,
                    billCount = state.categoryFormUsage,
                    movesTo = movesTo,
                    isDeleting = false,
                ),
            )
        }
    }

    // Why: back to the picker; a bill that had it reads as where its bills go.
    private fun delete() {
        val deletion = uiState.value.categoryDeletion?.takeIf { !it.isDeleting } ?: return
        uiState.update { it.copy(categoryDeletion = deletion.copy(isDeleting = true)) }
        scope.launch {
            deleteGroupCategory(deletion.category.id)
                .onSuccess {
                    uiState.update { state ->
                        val wasBills = state.category.id == deletion.category.id ||
                            state.category.parentId == deletion.category.id
                        state.copy(
                            categoryDeletion = null,
                            categoryForm = null,
                            categoryId = if (wasBills) deletion.movesTo.id else state.categoryId,
                        )
                    }
                    onCategoryChanged()
                }.onFailure {
                    uiState.update { it.copy(categoryDeletion = null) }
                    emit(SplitBillUiAction.ShowCategoryDeleteError)
                }
        }
    }

    private companion object {
        const val MAX_QUERY_LENGTH = CategoryRules.MAX_NAME_LENGTH
    }
}
