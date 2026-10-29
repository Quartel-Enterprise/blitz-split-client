package com.quare.blitzsplit.feature.deleteaccount.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.account.domain.usecase.ObserveAccountStatus
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.sync.domain.usecase.DeleteAccount
import com.quare.blitzsplit.core.sync.domain.usecase.ExportUserData
import com.quare.blitzsplit.feature.deleteaccount.presentation.model.DeleteAccountUiAction
import com.quare.blitzsplit.feature.deleteaccount.presentation.model.DeleteAccountUiEvent
import com.quare.blitzsplit.feature.deleteaccount.presentation.model.DeleteAccountUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

class DeleteAccountViewModel(
    private val navigator: Navigator,
    private val deleteAccount: DeleteAccount,
    private val exportUserData: ExportUserData,
    observeAccountStatus: ObserveAccountStatus,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {
    val uiState: StateFlow<DeleteAccountUiState>
        field = MutableStateFlow(
            DeleteAccountUiState(
                accountProvider = null,
                isOnline = true,
                hasConfirmed = false,
                isDeleting = false,
                hasFailed = false,
            ),
        )

    val uiAction: SharedFlow<DeleteAccountUiAction>
        field = MutableSharedFlow<DeleteAccountUiAction>()

    val stateWatcher: FlowWatcher<DeleteAccountUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<DeleteAccountUiAction> = FlowWatcher(uiAction)

    init {
        observeAccountStatus()
            .onEach { status ->
                uiState.update { it.copy(accountProvider = (status as? AccountStatus.Connected)?.provider) }
            }.launchIn(viewModelScope)
        connectivityObserver
            .observe()
            .onEach { isOnline -> uiState.update { it.copy(isOnline = isOnline) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: DeleteAccountUiEvent) {
        when (event) {
            DeleteAccountUiEvent.OnBackClicked -> leave()
            is DeleteAccountUiEvent.OnConfirmChanged -> confirm(event.isChecked)
            DeleteAccountUiEvent.OnDeleteClicked -> delete()
            DeleteAccountUiEvent.OnExportClicked -> export()
        }
    }

    // Why: once the deletion is under way the account may already be gone, so the screen waits for the welcome.
    private fun leave() {
        if (uiState.value.isDeleting) return
        if (navigator.backStack.value.lastOrNull() == Route.DeleteAccount) navigator.navigateBack()
    }

    private fun confirm(isChecked: Boolean) {
        if (!uiState.value.isConfirmEnabled) return
        uiState.update { it.copy(hasConfirmed = isChecked) }
    }

    // Why: a success clears the device and the app returns to the welcome, so only a failure needs handling.
    private fun delete() {
        if (!uiState.value.canDelete) return
        uiState.update {
            it.copy(
                isDeleting = true,
                hasFailed = false,
            )
        }
        viewModelScope.launch {
            deleteAccount().onFailure {
                uiState.update {
                    it.copy(
                        isDeleting = false,
                        hasFailed = true,
                    )
                }
            }
        }
    }

    private fun export() {
        // Why: like the profile's export, a failed read of the device's copy leaves the tap without a file.
        viewModelScope.launch {
            exportUserData().onSuccess { json -> uiAction.emit(DeleteAccountUiAction.SaveExport(json)) }
        }
    }
}
