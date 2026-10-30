package com.quare.blitzsplit.feature.editgroup.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveExpenses
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiEvent
import com.quare.blitzsplit.feature.editgroup.presentation.model.EditGroupUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class EditGroupViewModel(
    private val route: Route.EditGroup,
    private val navigator: Navigator,
    private val updateGroup: UpdateGroup,
    observeGroup: ObserveGroup,
    observeExpenses: ObserveExpenses,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<EditGroupUiState>
        field = MutableStateFlow(
            EditGroupUiState(
                name = "",
                initials = "",
                color = AvatarColor.default,
                type = GroupType.default,
                currency = GroupCurrency.fallback,
                isCurrencyLocked = false,
                isOnline = true,
                isLoaded = false,
                isSaving = false,
                showsSaveError = false,
                hasChanges = false,
            ),
        )

    val stateWatcher: FlowWatcher<EditGroupUiState> = FlowWatcher(uiState)

    // Why: the group went while another screen covered this one, which then leaves as soon as it is on top again.
    private var isGroupGone = false

    // Why: what the form opened with, so saving tells what this person changed from what others changed since.
    private var original: Group? = null

    // Why: the group as Room has it now, a sync from another member included.
    private var latest: Group? = null

    /*
     * Why: the form is filled from the group once, so a sync landing while someone types changes nothing here. Its
     * currency still follows the group's once a bill locks it, since the bill is in that currency.
     */
    init {
        combine(
            observeGroup(route.groupId),
            observeExpenses(route.groupId)
                .map { it.isNotEmpty() }
                .distinctUntilChanged(),
            ::Pair,
        ).onEach { (group, hasExpenses) ->
            isGroupGone = group == null
            if (group == null) leave() else show(group, hasExpenses)
        }.launchIn(viewModelScope)
        navigator.backStack
            .onEach { if (isGroupGone) leave() }
            .launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: EditGroupUiEvent) {
        when (event) {
            // Why: leaving clears the ViewModel, which would cancel a save in flight.
            EditGroupUiEvent.OnCloseClicked -> if (!uiState.value.isSaving) leave()

            is EditGroupUiEvent.OnNameChanged -> changeName(event.name)

            EditGroupUiEvent.OnColorClicked -> updateForm { it.copy(color = it.color.toNextColor()) }

            is EditGroupUiEvent.OnTypeSelected -> updateForm { it.copy(type = event.type) }

            is EditGroupUiEvent.OnCurrencySelected -> updateForm {
                if (it.isCurrencyLocked) it else it.copy(currency = event.currency)
            }

            EditGroupUiEvent.OnSaveClicked -> save()
        }
    }

    private fun show(
        group: Group,
        hasExpenses: Boolean,
    ) {
        latest = group
        if (original == null) original = group
        // Why: a locked currency follows the group's, which is no change of this person's once the bills go.
        if (hasExpenses) original = original?.copy(currency = group.currency)
        updateForm { state ->
            val filled = if (state.isLoaded) {
                state
            } else {
                state.copy(
                    name = group.name,
                    initials = Initials.of(group.name),
                    color = group.color,
                    type = group.type,
                    currency = group.currency,
                    isLoaded = true,
                )
            }
            filled.copy(
                isCurrencyLocked = hasExpenses,
                currency = if (hasExpenses) group.currency else filled.currency,
            )
        }
    }

    private fun changeName(name: String) {
        val capped = GroupNameRules.toCapped(name)
        updateForm {
            it.copy(
                name = capped,
                initials = Initials.of(capped),
            )
        }
    }

    private fun save() {
        val state = uiState.value
        val original = original ?: return
        val latest = latest ?: return
        if (!state.canSave) return
        uiState.update {
            it.copy(
                isSaving = true,
                showsSaveError = false,
            )
        }
        // Why: a field this person left as it opened keeps what another member changed it to meanwhile.
        val name = if (state.name.trim() != original.name) state.name else latest.name
        val type = if (state.type != original.type) state.type else latest.type
        val color = if (state.color != original.color) state.color else latest.color
        val currency = if (!state.isCurrencyLocked && state.currency != original.currency) {
            state.currency
        } else {
            latest.currency
        }
        viewModelScope.launch {
            updateGroup(
                groupId = route.groupId,
                name = name,
                type = type,
                color = color,
                currency = currency,
            ).onSuccess {
                // Why: the form may not be on top to leave, so it stops saving and starts from what it just saved.
                this@EditGroupViewModel.original = latest.copy(
                    name = name.trim(),
                    type = type,
                    color = color,
                    currency = currency,
                )
                updateForm { it.copy(isSaving = false) }
                leave()
            }.onFailure {
                uiState.update {
                    it.copy(
                        isSaving = false,
                        showsSaveError = true,
                    )
                }
            }
        }
    }

    // Why: every change to the form says whether it still differs from what it opened with, which enables Salvar.
    private fun updateForm(transform: (EditGroupUiState) -> EditGroupUiState) {
        uiState.update { state ->
            val next = transform(state)
            next.copy(hasChanges = next.isChangedFrom(original))
        }
    }

    private fun EditGroupUiState.isChangedFrom(group: Group?): Boolean = group != null &&
        (
            name.trim() != group.name ||
                type != group.type ||
                color != group.color ||
                (!isCurrencyLocked && currency != group.currency)
        )

    private fun AvatarColor.toNextColor(): AvatarColor = AvatarColor.entries[(ordinal + 1) % AvatarColor.entries.size]

    // Why: only while it is still on top: a native back (a swipe, the browser's, a sheet dismissed) may have closed it.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == route) navigator.navigateBack()
    }
}
