package com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveNotificationPreferences
import com.quare.blitzsplit.core.profile.domain.usecase.UpdateNotificationPreference
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationPermission
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiAction
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
 * Why: only the choices and the system permission. Nothing sends a push or registers the device yet; that comes
 * with the push sender, which reads the synced choices.
 */
class NotificationSettingsViewModel(
    private val navigator: Navigator,
    private val updateNotificationPreference: UpdateNotificationPreference,
    observeNotificationPreferences: ObserveNotificationPreferences,
) : ViewModel() {
    val uiState: StateFlow<NotificationSettingsUiState>
        field = MutableStateFlow(
            NotificationSettingsUiState(
                enabledTypes = NotificationPreferences.DEFAULT.enabledTypes,
                permission = null,
            ),
        )

    val uiAction: SharedFlow<NotificationSettingsUiAction>
        field = MutableSharedFlow<NotificationSettingsUiAction>()

    val stateWatcher: FlowWatcher<NotificationSettingsUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<NotificationSettingsUiAction> = FlowWatcher(uiAction)

    init {
        observeNotificationPreferences()
            .onEach { preferences -> uiState.update { it.copy(enabledTypes = preferences.enabledTypes) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: NotificationSettingsUiEvent) {
        when (event) {
            NotificationSettingsUiEvent.OnBackClicked -> leave()

            is NotificationSettingsUiEvent.OnPermissionChecked -> uiState.update {
                it.copy(
                    permission = event.permission,
                )
            }

            is NotificationSettingsUiEvent.OnToggled -> toggle(event.type, event.isEnabled)

            NotificationSettingsUiEvent.OnOpenSettingsClicked -> emit(NotificationSettingsUiAction.OpenSystemSettings)
        }
    }

    // Why: the web and macOS draw it inside the profile, where the back stack doesn't end on this screen.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == Route.NotificationSettings) navigator.navigateBack()
    }

    /*
     * Why: the system is asked only once a notice is turned on, and only if it never was. With the permission
     * denied the toggles are off and disabled, so a tap that still arrives changes nothing.
     */
    private fun toggle(
        type: NotificationType,
        isEnabled: Boolean,
    ) {
        val state = uiState.value
        if (!state.areTogglesEnabled) return
        viewModelScope.launch {
            updateNotificationPreference(
                type = type,
                isEnabled = isEnabled,
            )
        }
        if (isEnabled && state.permission == NotificationPermission.NOT_DETERMINED) {
            emit(NotificationSettingsUiAction.RequestPermission)
        }
    }

    private fun emit(action: NotificationSettingsUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }
}
