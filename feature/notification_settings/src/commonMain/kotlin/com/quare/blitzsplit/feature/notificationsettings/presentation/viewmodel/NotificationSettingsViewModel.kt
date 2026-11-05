package com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveNotificationPreferences
import com.quare.blitzsplit.core.profile.domain.usecase.UpdateNotificationPreference
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationPermission
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiAction
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiEvent
import com.quare.blitzsplit.feature.notificationsettings.presentation.model.NotificationSettingsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded

/*
 * Why: the choices and the system permission, for the Push and In app channel of each row. The server reads the
 * synced push choices to send; Activity reads the in-app ones for its bell. Opened from the profile and from Activity.
 */
class NotificationSettingsViewModel(
    private val navigator: Navigator,
    private val updateNotificationPreference: UpdateNotificationPreference,
    observeNotificationPreferences: ObserveNotificationPreferences,
    observeFirstDownload: ObserveFirstDownload,
    trackEvent: TrackEvent,
) : TrackedViewModel<NotificationSettingsUiEvent>(trackEvent) {
    val uiState: StateFlow<NotificationSettingsUiState>
        field = MutableStateFlow(
            NotificationSettingsUiState(
                preferences = Loadable.Loading,
                permission = null,
            ),
        )

    val uiAction: SharedFlow<NotificationSettingsUiAction>
        field = MutableSharedFlow<NotificationSettingsUiAction>()

    val stateWatcher: FlowWatcher<NotificationSettingsUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<NotificationSettingsUiAction> = FlowWatcher(uiAction)

    init {
        combine(observeNotificationPreferences(), observeFirstDownload()) { preferences, firstDownload ->
            if (firstDownload != null) Loadable.Loading else preferences.toLoaded()
        }.onEach { preferences -> uiState.update { it.copy(preferences = preferences) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: NotificationSettingsUiEvent) {
        when (event) {
            NotificationSettingsUiEvent.OnBackClicked -> leave()

            is NotificationSettingsUiEvent.OnPermissionChecked -> uiState.update {
                it.copy(
                    permission = event.permission,
                )
            }

            is NotificationSettingsUiEvent.OnToggled -> toggle(event.type, event.channel, event.isEnabled)

            NotificationSettingsUiEvent.OnOpenSettingsClicked -> emit(NotificationSettingsUiAction.OpenSystemSettings)
        }
    }

    // Why: the web and macOS draw it inside the profile, where the back stack doesn't end on this screen.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == Route.NotificationSettings) navigator.navigateBack()
    }

    /*
     * Why: the system is asked only once a push is turned on, and only if it never was; the bell needs no permission.
     * With the choices still loading or the permission unknown (or, for a push, denied), the toggles are disabled,
     * so a tap that still arrives changes nothing.
     */
    private fun toggle(
        type: NotificationType,
        channel: NotificationChannel,
        isEnabled: Boolean,
    ) {
        val state = uiState.value
        if (!state.areTogglesEnabled(channel) || !state.hasToggle(type, channel)) return
        trackEvent("notification_toggled", mapOf("type" to type, "channel" to channel, "is_enabled" to isEnabled))
        viewModelScope.launch {
            updateNotificationPreference(
                type = type,
                channel = channel,
                isEnabled = isEnabled,
            )
        }
        if (isEnabled && channel == NotificationChannel.PUSH &&
            state.permission == NotificationPermission.NOT_DETERMINED
        ) {
            emit(NotificationSettingsUiAction.RequestPermission)
        }
    }

    private fun emit(action: NotificationSettingsUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }
}
