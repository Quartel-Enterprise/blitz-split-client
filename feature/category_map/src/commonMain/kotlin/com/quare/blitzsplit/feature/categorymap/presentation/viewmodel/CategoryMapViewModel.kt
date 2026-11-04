package com.quare.blitzsplit.feature.categorymap.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccount
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryMap
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.usecase.DeleteGroupCategory
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveCategoryMap
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsByActivity
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveMapCategories
import com.quare.blitzsplit.core.expenses.domain.usecase.RemoveCategoryTerm
import com.quare.blitzsplit.core.expenses.domain.usecase.RestoreCategoryMap
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveCategoryTerm
import com.quare.blitzsplit.core.expenses.domain.usecase.SaveGroupCategory
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiAction
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiEvent
import com.quare.blitzsplit.feature.categorymap.presentation.model.CategoryMapUiState
import com.quare.blitzsplit.feature.categorymap.presentation.model.MapGroup
import com.quare.blitzsplit.feature.categorymap.presentation.model.TermForm
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
    saveGroupCategory: SaveGroupCategory,
    deleteGroupCategory: DeleteGroupCategory,
    observeCategoryMap: ObserveCategoryMap,
    observeMapCategories: ObserveMapCategories,
    observeGroupsByActivity: ObserveGroupsByActivity,
    observeExpenses: ObserveExpenses,
    observeAccount: ObserveAccount,
    observeFirstDownload: ObserveFirstDownload,
    connectivityObserver: NetworkConnectivityObserver,
    trackEvent: TrackEvent,
) : TrackedViewModel<CategoryMapUiEvent>(trackEvent) {
    val uiState: StateFlow<CategoryMapUiState>
        field = MutableStateFlow(
            CategoryMapUiState(
                isLoaded = false,
                map = CategoryMap.of(emptyList()),
                categories = CategoryTree.appOnly,
                groups = emptyList(),
                query = "",
                selectedCategoryId = null,
                termForm = null,
                removedTerm = null,
                isRestoreConfirmOpen = false,
                categoryForm = null,
                categoryDeletion = null,
                categoryBills = null,
                isOnline = true,
                isConnected = false,
            ),
        )

    val uiAction: SharedFlow<CategoryMapUiAction>
        field = MutableSharedFlow<CategoryMapUiAction>()

    val stateWatcher: FlowWatcher<CategoryMapUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<CategoryMapUiAction> = FlowWatcher(uiAction)

    private val categoryEditing = MapCategoryEditing(
        uiState = uiState,
        scope = viewModelScope,
        saveGroupCategory = saveGroupCategory,
        deleteGroupCategory = deleteGroupCategory,
        emit = uiAction::emit,
        trackEvent = trackEvent,
    )

    init {
        // Why: one read, so the terms never show against the app's categories alone, nor without the account.
        combine(
            observeCategoryMap(),
            observeMapCategories(),
            // Why: a bill or payment that moves neither a group nor their order leaves the map as it is.
            observeGroupsByActivity().distinctUntilChanged(),
            observeAccount().map { account -> account.status is AccountStatus.Connected }.distinctUntilChanged(),
            observeFirstDownload().map { it == null }.distinctUntilChanged(),
        ) { map, tree, groups, isConnected, isWhole ->
            val mapGroups = groups.map { group ->
                MapGroup(
                    id = group.id,
                    name = group.name,
                    memberNames = group.members.associate { member -> member.id to member.name },
                    meId = group.members.firstOrNull { member -> member.isMe }?.id,
                )
            }
            uiState.update {
                // Why: a form whose group left the device (deleted or left elsewhere) has nowhere to save to.
                val form = it.categoryForm?.takeIf { form -> mapGroups.any { group -> group.id == form.groupId } }
                it.copy(
                    isLoaded = isWhole,
                    map = map,
                    categories = tree,
                    groups = mapGroups,
                    categoryForm = form,
                    categoryDeletion = it.categoryDeletion?.takeIf { form != null },
                    isConnected = isConnected,
                )
            }
        }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
        categoryEditing.observe(observeExpenses)
    }

    override fun handleEvent(event: CategoryMapUiEvent) {
        if (!uiState.value.canEditMap && event.isMapChange()) return
        when (event) {
            CategoryMapUiEvent.OnBackClicked -> leave()

            CategoryMapUiEvent.OnProfileClicked -> openProfile()

            is CategoryMapUiEvent.OnQueryChanged -> uiState.update { it.copy(query = event.text) }

            is CategoryMapUiEvent.OnSectionSelected -> if (event.categoryId != uiState.value.selectedCategoryId) {
                trackEvent("category_map_section_selected", emptyMap())
                uiState.update { it.copy(selectedCategoryId = event.categoryId) }
            }

            is CategoryMapUiEvent.OnAddTermClicked -> {
                trackEvent("category_term_add_clicked", mapOf("entry" to event.entry))
                openNewTerm(event.categoryId, event.text)
            }

            is CategoryMapUiEvent.OnTermClicked -> {
                trackEvent("category_term_clicked", emptyMap())
                openTerm(event.key)
            }

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

            CategoryMapUiEvent.OnTermSaveClicked -> {
                trackEvent("category_term_save_clicked", emptyMap())
                saveTerm()
            }

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

            CategoryMapUiEvent.OnRestoreClicked -> {
                trackEvent("category_map_restore_clicked", emptyMap())
                uiState.update { it.copy(isRestoreConfirmOpen = true) }
            }

            CategoryMapUiEvent.OnRestoreConfirmed -> {
                if (uiState.value.isRestoreConfirmOpen) trackEvent("category_map_restore_confirmed", emptyMap())
                restore()
            }

            CategoryMapUiEvent.OnRestoreDismissed -> {
                if (uiState.value.isRestoreConfirmOpen) trackEvent("category_map_restore_cancelled", emptyMap())
                uiState.update { it.copy(isRestoreConfirmOpen = false) }
            }

            is CategoryMapUiEvent.Categories -> categoryEditing.onEvent(event)
        }
    }

    private fun CategoryMapUiEvent.isMapChange(): Boolean = this is CategoryMapUiEvent.OnAddTermClicked ||
        this is CategoryMapUiEvent.OnTermClicked ||
        this == CategoryMapUiEvent.OnUndoRemoveClicked ||
        this == CategoryMapUiEvent.OnRestoreClicked ||
        this == CategoryMapUiEvent.OnRestoreConfirmed ||
        this is CategoryMapUiEvent.OnNewCategoryClicked ||
        this is CategoryMapUiEvent.OnEditCategoryClicked

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
                trackEvent("category_term_saved", mapOf("is_new" to (form.original == null)))
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
