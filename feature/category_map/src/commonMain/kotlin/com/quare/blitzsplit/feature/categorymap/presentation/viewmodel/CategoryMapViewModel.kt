package com.quare.blitzsplit.feature.categorymap.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMap
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryMap
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMapCategories
import com.quare.blitzsplit.core.expenses.domain.usecase.RemoveCategoryTerm
import com.quare.blitzsplit.core.expenses.domain.usecase.RestoreCategoryMap
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveCategoryTerm
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiAction
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiState
import com.quare.blitzsplit.feature.categorymap.presentation.model.TermForm
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

// Why: the person's map works offline and counts at once on this device; a sync takes it to their others (flow 9).
class CategoryMapViewModel(
    private val navigator: Navigator,
    private val saveCategoryTerm: SaveCategoryTerm,
    private val removeCategoryTerm: RemoveCategoryTerm,
    private val restoreCategoryMap: RestoreCategoryMap,
    observeCategoryMap: ObserveCategoryMap,
    observeMapCategories: ObserveMapCategories,
    observeGroups: ObserveGroups,
    observeAccount: ObserveAccount,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<CategoryMapUiState>
        field = MutableStateFlow(
            CategoryMapUiState(
                isLoaded = false,
                map = CategoryMap.of(emptyList()),
                categories = CategoryTree.appOnly,
                groupNames = emptyMap(),
                query = "",
                selectedCategoryId = null,
                termForm = null,
                removedTerm = null,
                isRestoreConfirmOpen = false,
                isOnline = true,
                isConnected = false,
            ),
        )

    val uiAction: SharedFlow<CategoryMapUiAction>
        field = MutableSharedFlow<CategoryMapUiAction>()

    val stateWatcher: FlowWatcher<CategoryMapUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<CategoryMapUiAction> = FlowWatcher(uiAction)

    init {
        observeCategoryMap()
            .onEach { map ->
                uiState.update {
                    it.copy(
                        map = map,
                        isLoaded = true,
                    )
                }
            }.launchIn(viewModelScope)
        observeMapCategories()
            .onEach { tree -> uiState.update { it.copy(categories = tree) } }
            .launchIn(viewModelScope)
        observeGroups()
            .onEach { groups ->
                uiState.update {
                    it.copy(
                        groupNames = groups.associate { group ->
                            group.id to group.name
                        },
                    )
                }
            }.launchIn(viewModelScope)
        observeAccount()
            .onEach { account -> uiState.update { it.copy(isConnected = account.status is AccountStatus.Connected) } }
            .launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: CategoryMapUiEvent) {
        when (event) {
            CategoryMapUiEvent.OnBackClicked -> leave()

            CategoryMapUiEvent.OnProfileClicked -> openProfile()

            is CategoryMapUiEvent.OnQueryChanged -> uiState.update { it.copy(query = event.text) }

            is CategoryMapUiEvent.OnSectionSelected -> uiState.update { it.copy(selectedCategoryId = event.categoryId) }

            is CategoryMapUiEvent.OnAddTermClicked -> openNewTerm(event.categoryId, event.text)

            is CategoryMapUiEvent.OnTermClicked -> openTerm(event.key)

            is CategoryMapUiEvent.OnTermTextChanged -> uiState.update { state ->
                state.copy(termForm = state.termForm?.copy(text = event.text.take(MAX_TYPED)))
            }

            CategoryMapUiEvent.OnTermCategoryClicked -> uiState.update { state ->
                state.copy(termForm = state.termForm?.let { it.copy(isPickingCategory = !it.isPickingCategory) })
            }

            is CategoryMapUiEvent.OnTermCategoryPicked -> uiState.update { state ->
                val picked = state.categories.findOrNull(event.categoryId) ?: return@update state
                state.copy(
                    termForm = state.termForm?.copy(
                        categoryId = picked.id,
                        isPickingCategory = false,
                    ),
                )
            }

            CategoryMapUiEvent.OnTermSaveClicked -> saveTerm()

            CategoryMapUiEvent.OnTermRemoveClicked -> removeTerm()

            CategoryMapUiEvent.OnTermDismissed -> uiState.update {
                if (it.termForm?.isSaving ==
                    true
                ) {
                    it
                } else {
                    it.copy(termForm = null)
                }
            }

            CategoryMapUiEvent.OnUndoRemoveClicked -> undoRemove()

            CategoryMapUiEvent.OnRemovedNoticeDismissed -> uiState.update { it.copy(removedTerm = null) }

            CategoryMapUiEvent.OnRestoreClicked -> uiState.update { it.copy(isRestoreConfirmOpen = true) }

            CategoryMapUiEvent.OnRestoreConfirmed -> restore()

            CategoryMapUiEvent.OnRestoreDismissed -> uiState.update { it.copy(isRestoreConfirmOpen = false) }
        }
    }

    // Why: the Mac draws it in Ajustes, where the back stack doesn't end on this screen.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == Route.CategoryMap) navigator.navigateBack()
    }

    private fun openProfile() {
        val stack = navigator.backStack.value
        if (stack.lastOrNull() == Route.CategoryMap && stack.getOrNull(stack.lastIndex - 1) == Route.Profile) {
            navigator.navigateBack()
        } else {
            navigator.navigate(Route.Profile)
        }
    }

    // Why: a new term starts in the category it was added from, else the one picked beside the list, else Outros.
    private fun openNewTerm(
        categoryId: String?,
        text: String,
    ) {
        uiState.update { state ->
            val start = categoryId?.let { state.categories.findOrNull(it) }
                ?: state.selectedSection?.category?.takeIf {
                    categoryId == null && state.sections.size > 1 &&
                        !state.isSearching
                }
                ?: state.categories.findOrNull(AppCategory.OTHER.key)
            state.copy(
                termForm = TermForm(
                    original = null,
                    text = text.trim().take(MAX_TYPED),
                    categoryId = start?.id ?: AppCategory.OTHER.key,
                    isPickingCategory = false,
                    isSaving = false,
                ),
            )
        }
    }

    private fun openTerm(key: String) {
        uiState.update { state ->
            val term = state.findTermOrNull(key) ?: return@update state
            state.copy(
                termForm = TermForm(
                    original = term,
                    text = term.term,
                    categoryId = term.categoryId,
                    isPickingCategory = false,
                    isSaving = false,
                ),
            )
        }
    }

    private fun saveTerm() {
        val state = uiState.value
        val form = state.termForm ?: return
        if (!state.canSaveTerm) return
        uiState.update { it.copy(termForm = form.copy(isSaving = true)) }
        viewModelScope.launch {
            saveCategoryTerm(
                term = form.text.trim(),
                categoryId = form.categoryId,
                replacedTerm = form.original?.term,
            ).onSuccess {
                uiState.update {
                    it.copy(
                        termForm = null,
                        removedTerm = null,
                    )
                }
            }.onFailure {
                uiState.update { it.copy(termForm = it.termForm?.copy(isSaving = false)) }
                uiAction.emit(CategoryMapUiAction.ShowSaveError)
            }
        }
    }

    // Why: no confirmation; "Desfazer" brings it back where it was (9.1d).
    private fun removeTerm() {
        val state = uiState.value
        val shown = state.termForm?.original ?: return
        // Why: Desfazer brings back the category stored, a deleted group category's included, not the one it reads as.
        val term = state.map.findOrNull(shown.key) ?: shown
        uiState.update { it.copy(termForm = null) }
        viewModelScope.launch {
            removeCategoryTerm(term.term)
                .onSuccess { uiState.update { it.copy(removedTerm = term) } }
                .onFailure { uiAction.emit(CategoryMapUiAction.ShowSaveError) }
        }
    }

    private fun undoRemove() {
        val term = uiState.value.removedTerm ?: return
        uiState.update { it.copy(removedTerm = null) }
        viewModelScope.launch {
            saveCategoryTerm(
                term = term.term,
                categoryId = term.categoryId,
                replacedTerm = null,
            ).onFailure { uiAction.emit(CategoryMapUiAction.ShowSaveError) }
        }
    }

    private fun restore() {
        uiState.update {
            it.copy(
                isRestoreConfirmOpen = false,
                removedTerm = null,
            )
        }
        viewModelScope.launch {
            restoreCategoryMap().onFailure { uiAction.emit(CategoryMapUiAction.ShowSaveError) }
        }
    }

    private companion object {
        // Why: room past the 30 letters a term may have, so what is typed shows why it can't be saved.
        const val MAX_TYPED = 40
    }
}
