package com.quare.blitzsplit.feature.splitbill.presentation.viewmodel

import com.quare.blitzsplit.core.expenses.customcolor.CustomColorController
import com.quare.blitzsplit.core.expenses.domain.model.CategoryRules
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTerm
import com.quare.blitzsplit.core.expenses.domain.model.DefaultCategoryTerms
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseRules
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.invoke
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SPLIT_BILL_SOURCE
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiAction
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiEvent
import com.quare.blitzsplit.feature.splitbill.presentation.model.SplitBillUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private val useCases: BillCategoryUseCases,
    private val observeExpenses: ObserveExpenses,
    private val emit: suspend (SplitBillUiAction) -> Unit,
    private val onCategoryChanged: () -> Unit,
    private val trackEvent: TrackEvent,
) {
    // Why: the person's map (flow 9), whose words suggest a bill's category from its description.
    var terms: List<CategoryTerm> = DefaultCategoryTerms.terms
        private set

    // Why: "Add" in "New color" gives the color to the form open on screen.
    private val customColors = CustomColorController(
        useCases = useCases.customColors,
        scope = scope,
        onPicked = { tint, _ -> uiState.update { it.copy(categoryForm = it.categoryForm?.withColor(tint)) } },
    )

    // Why: the group's categories and how its bills use them, as a sync or a change here moves them.
    fun observe(groupId: Flow<String>) {
        customColors.observe()
        customColors.state
            .onEach { colors -> uiState.update { it.copy(customColors = colors) } }
            .launchIn(scope)
        groupId
            .flatMapLatest { id -> combine(useCases.observeCategoryTree(id), observeExpenses(id), ::Pair) }
            .onEach { (tree, expenses) -> change { it.withCategories(tree, expenses).withCategoryMatched(terms) } }
            .launchIn(scope)
        // Why: a term changed in the map, on this device or synced, chooses again while the category isn't picked.
        useCases
            .observeCategoryMap()
            .onEach { map ->
                terms = map.terms.map { it.asTerm }
                change { it.withCategoryMatched(terms) }
            }.launchIn(scope)
    }

    // Why: until the category is picked by hand, the description's words choose it (7.1).
    fun changeDescription(text: String) {
        change {
            it
                .copy(
                    description = ExpenseRules.toCapped(text),
                    hasEditedDescription = true,
                ).withCategoryMatched(terms)
        }
    }

    // Why: a receipt read or removed (7.1e) changes the description and the category it suggests, so the words choose again.
    fun rematch(transform: (SplitBillUiState) -> SplitBillUiState) {
        change { transform(it).withCategoryMatched(terms) }
    }

    fun onEvent(event: SplitBillUiEvent.Categories) {
        when (event) {
            SplitBillUiEvent.OnCategoryClicked -> {
                if (uiState.value.isLoaded) trackEvent("bill_category_clicked", emptyMap())
                uiState.update { if (it.isLoaded) it.withCategoryPickerOpened() else it }
            }

            is SplitBillUiEvent.OnCategoryQueryChanged -> uiState.update { state ->
                state.copy(categoryPicker = state.categoryPicker?.copy(query = event.text.take(MAX_QUERY_LENGTH)))
            }

            is SplitBillUiEvent.OnCategoryMainToggled -> toggleMain(event.categoryId)

            // Why: only a category still there, never one a sync just deleted.
            is SplitBillUiEvent.OnCategoryPicked -> {
                val before = uiState.value
                change { it.withCategoryPicked(event.categoryId) }
                if (uiState.value != before) trackEvent("bill_category_picked", emptyMap())
            }

            SplitBillUiEvent.OnCategoryDismissed -> uiState.update {
                it.copy(
                    categoryPicker = null,
                    categoryForm = null,
                )
            }

            is SplitBillUiEvent.OnNewCategoryClicked -> uiState.update {
                it.withNewCategoryForm(
                    event.parentId,
                    event.name,
                )
            }

            // Why: only a category its form opens for, one of the group's still there.
            is SplitBillUiEvent.OnEditCategoryClicked -> {
                val before = uiState.value.categoryForm
                uiState.update { it.withEditCategoryForm(event.categoryId) }
                if (uiState.value.categoryForm.let { it != null && it != before }) {
                    trackEvent("category_edit_clicked", mapOf("source" to SPLIT_BILL_SOURCE))
                }
            }

            is SplitBillUiEvent.OnCategoryNameChanged -> uiState.update { state ->
                state.copy(categoryForm = state.categoryForm?.withName(event.text))
            }

            is SplitBillUiEvent.OnCategoryParentPicked -> uiState.update { state ->
                state.copy(categoryForm = state.categoryForm?.withParent(state.categories, event.parentId))
            }

            is SplitBillUiEvent.OnCategoryIconPicked -> uiState.update { state ->
                state.copy(categoryForm = state.categoryForm?.withIcon(event.icon))
            }

            is SplitBillUiEvent.OnCategoryColorPicked -> uiState.update { state ->
                state.copy(categoryForm = state.categoryForm?.withColor(event.color))
            }

            is SplitBillUiEvent.OnCustomColor -> customColors.onEvent(event.event)

            SplitBillUiEvent.OnCategoryFormSaveClicked -> {
                trackEvent("category_save_clicked", mapOf("source" to SPLIT_BILL_SOURCE))
                save()
            }

            SplitBillUiEvent.OnCategoryFormDismissed -> uiState.update {
                if (it.categoryForm?.isSaving == true) it else it.copy(categoryForm = null)
            }

            SplitBillUiEvent.OnCategoryDeleteClicked -> askToDelete()

            SplitBillUiEvent.OnCategoryDeleteConfirmed -> delete()

            // Why: only a question still open and not deleting yet.
            SplitBillUiEvent.OnCategoryDeleteDismissed -> {
                val deletion = uiState.value.categoryDeletion
                if (deletion != null && !deletion.isDeleting) {
                    trackEvent("category_delete_cancelled", mapOf("source" to SPLIT_BILL_SOURCE))
                }
                uiState.update {
                    if (it.categoryDeletion?.isDeleting == true) it else it.copy(categoryDeletion = null)
                }
            }
        }
    }

    /*
     * Why: a main category shown or hidden again, with where it ended up. Outros, which has no subcategories, is picked
     * instead and closes the picker, so it counts as a pick.
     */
    private fun toggleMain(mainId: String) {
        val before = uiState.value.categoryPicker ?: return
        change { it.withCategoryMainToggled(mainId) }
        val after = uiState.value.categoryPicker
        if (after == null) {
            trackEvent("bill_category_picked", emptyMap())
        } else if (after.openMainId != before.openMainId) {
            trackEvent("bill_category_main_toggled", mapOf("is_expanded" to (after.openMainId == mainId)))
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
        if (state.categoryEditor?.canSave != true) return
        uiState.update { it.copy(categoryForm = form.copy(isSaving = true)) }
        val meId = state.people.firstOrNull { it.isMe }?.id
        scope.launch {
            useCases
                .saveGroupCategory(form, meId)
                .onSuccess { id ->
                    trackEvent(
                        "category_saved",
                        mapOf(
                            "is_new" to !form.isEditing,
                            "is_subcategory" to (form.parentId != null),
                            "source" to SPLIT_BILL_SOURCE,
                        ),
                    )
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
        val deletion = uiState.value.categoryEditor?.findDeletionOrNull() ?: return
        trackEvent("category_delete_clicked", mapOf("source" to SPLIT_BILL_SOURCE))
        uiState.update { it.copy(categoryDeletion = deletion) }
    }

    // Why: back to the picker; a bill that had it reads as where its bills go.
    private fun delete() {
        val before = uiState.value
        val deletion = before.categoryDeletion?.takeIf { !it.isDeleting } ?: return
        trackEvent("category_delete_confirmed", mapOf("source" to SPLIT_BILL_SOURCE))
        // Why: read before the delete, as Room's tombstone may reach the tree, and re-resolve the bill, first.
        val wasBills = before.category.id == deletion.category.id || before.category.parentId == deletion.category.id
        uiState.update { it.copy(categoryDeletion = deletion.copy(isDeleting = true)) }
        scope.launch {
            useCases
                .deleteGroupCategory(deletion.category.id)
                .onSuccess {
                    uiState.update { state ->
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
