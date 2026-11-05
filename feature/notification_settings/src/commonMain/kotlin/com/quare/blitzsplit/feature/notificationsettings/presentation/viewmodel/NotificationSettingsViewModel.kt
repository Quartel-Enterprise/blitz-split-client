package com.quare.blitzsplit.feature.notificationsettings.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.quare.blitzsplit.core.activity.domain.usecase.MarkActivityTypeRead
import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.navigation.Navigator
import com.quare.blitzsplit.core.navigation.Route
import com.quare.blitzsplit.core.presentation.analytics.TrackedViewModel
import com.quare.blitzsplit.core.presentation.bridge.FlowWatcher
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveNotificationPreferences
import com.quare.blitzsplit.core.profile.domain.usecase.UpdateNotificationPreference
import com.quare.blitzsplit.core.provider.analytics.domain.usecase.TrackEvent
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
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
    private val markActivityTypeRead: MarkActivityTypeRead,
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

    // Why: the app asked the system, and the next check of the permission says how it answered.
    private var awaitsPermissionAnswer = false

    init {
        combine(observeNotificationPreferences(), observeFirstDownload()) { preferences, firstDownload ->
            if (firstDownload != null) Loadable.Loading else preferences.toLoaded()
        }.onEach { preferences -> uiState.update { it.copy(preferences = preferences) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: NotificationSettingsUiEvent) {
        when (event) {
            NotificationSettingsUiEvent.OnBackClicked -> leave()

            is NotificationSettingsUiEvent.OnPermissionChecked -> checkPermission(event.permission)

            is NotificationSettingsUiEvent.OnSystemBlockedTypesChecked -> uiState.update {
                it.copy(systemBlockedTypes = event.types)
            }

            is NotificationSettingsUiEvent.OnToggled -> toggle(event.type, event.channel, event.isEnabled)

            NotificationSettingsUiEvent.OnOpenSettingsClicked -> emit(NotificationSettingsUiAction.OpenSystemSettings)

            NotificationSettingsUiEvent.OnAskAgainClicked -> if (uiState.value.canAskAgain) {
                trackEvent("notification_ask_again_clicked", emptyMap())
                requestPermission()
            }

            NotificationSettingsUiEvent.OnQuietTurnOnClicked -> if (uiState.value.showsQuietNotice) {
                trackEvent("notification_quiet_turn_on_clicked", emptyMap())
                requestPermission()
            }

            is NotificationSettingsUiEvent.OnChannelSettingsClicked -> if (uiState.value.isBlockedBySystem(event.type)) {
                trackEvent("notification_channel_settings_clicked", mapOf("type" to event.type))
                emit(NotificationSettingsUiAction.OpenChannelSettings(event.type))
            }
        }
    }

    // Why: the web and macOS draw it inside the profile, where the back stack doesn't end on this screen.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == Route.NotificationSettings) navigator.navigateBack()
    }

    /*
     * Why: the system is asked only once a push is turned on, and only if it never was (or Apple delivers quietly); the
     * bell needs no permission. With the choices still loading or the permission unknown (or, for a push, denied, all or
     * the row's channel), the toggles are disabled, so a tap that still arrives changes nothing.
     */
    private fun toggle(
        type: NotificationType,
        channel: NotificationChannel,
        isEnabled: Boolean,
    ) {
        val state = uiState.value
        if (!state.isToggleEnabled(type, channel) || !state.hasToggle(type, channel)) return
        trackEvent("notification_toggled", mapOf("type" to type, "channel" to channel, "is_enabled" to isEnabled))
        viewModelScope.launch {
            // Why: what came while the bell was off stays read once it is back on, as it showed meanwhile.
            if (channel == NotificationChannel.IN_APP && isEnabled) markActivityTypeRead(type)
            updateNotificationPreference(
                type = type,
                channel = channel,
                isEnabled = isEnabled,
            )
        }
        // Why: Apple's full prompt too while it delivers quietly, for banners and sounds.
        if (isEnabled && channel == NotificationChannel.PUSH &&
            (state.permission == NotificationPermission.NOT_DETERMINED || state.showsQuietNotice)
        ) {
            requestPermission()
        }
    }

    private fun requestPermission() {
        awaitsPermissionAnswer = true
        emit(NotificationSettingsUiAction.RequestPermission)
    }

    private fun checkPermission(permission: NotificationPermission) {
        uiState.update { it.copy(permission = permission) }
        if (awaitsPermissionAnswer) {
            awaitsPermissionAnswer = false
            trackEvent(
                "notification_permission_answered",
                mapOf(
                    "source" to "notification_settings",
                    "is_granted" to (permission == NotificationPermission.GRANTED),
                ),
            )
        }
    }

    private fun emit(action: NotificationSettingsUiAction) {
        viewModelScope.launch { uiAction.emit(action) }
    }
}
