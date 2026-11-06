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
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePushEnabled
import com.quare.blitzsplit.core.profile.domain.usecase.UpdateNotificationPreference
import com.quare.blitzsplit.core.profile.domain.usecase.UpdatePushEnabled
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
 * Why: the choices and the system permission, for the Push and In app channel of each row, under the switch that lets
 * this device take pushes at all. The server reads the synced push choices to send; Activity reads the in-app ones for
 * its bell. Opened from the profile and from Activity.
 */
class NotificationSettingsViewModel(
    private val navigator: Navigator,
    private val updateNotificationPreference: UpdateNotificationPreference,
    private val markActivityTypeRead: MarkActivityTypeRead,
    private val updatePushEnabled: UpdatePushEnabled,
    observeNotificationPreferences: ObserveNotificationPreferences,
    observePushEnabled: ObservePushEnabled,
    observeFirstDownload: ObserveFirstDownload,
    trackEvent: TrackEvent,
) : TrackedViewModel<NotificationSettingsUiEvent>(trackEvent) {
    val uiState: StateFlow<NotificationSettingsUiState>
        field = MutableStateFlow(
            NotificationSettingsUiState(
                preferences = Loadable.Loading,
                devicePush = Loadable.Loading,
                permission = null,
                systemBlockedTypes = emptySet(),
            ),
        )

    val uiAction: SharedFlow<NotificationSettingsUiAction>
        field = MutableSharedFlow<NotificationSettingsUiAction>()

    val stateWatcher: FlowWatcher<NotificationSettingsUiState> = FlowWatcher(uiState)
    val actionWatcher: FlowWatcher<NotificationSettingsUiAction> = FlowWatcher(uiAction)

    // Why: the app asked the system, and its answer (OnPermissionAnswered) hasn't come back yet.
    private var awaitsPermissionAnswer = false

    init {
        combine(observeNotificationPreferences(), observeFirstDownload()) { preferences, firstDownload ->
            if (firstDownload != null) Loadable.Loading else preferences.toLoaded()
        }.onEach { preferences -> uiState.update { it.copy(preferences = preferences) } }
            .launchIn(viewModelScope)
        observePushEnabled()
            .onEach { isEnabled -> uiState.update { it.copy(devicePush = isEnabled.toLoaded()) } }
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

            is NotificationSettingsUiEvent.OnPermissionAnswered -> answerPermission(event.permission)

            is NotificationSettingsUiEvent.OnSystemBlockedTypesChecked -> uiState.update {
                it.copy(systemBlockedTypes = event.types)
            }

            is NotificationSettingsUiEvent.OnToggled -> toggle(event.type, event.channel, event.isEnabled)

            is NotificationSettingsUiEvent.OnPushToggled -> togglePush(event.isEnabled)

            NotificationSettingsUiEvent.OnOpenSettingsClicked -> emit(NotificationSettingsUiAction.OpenSystemSettings)

            NotificationSettingsUiEvent.OnAskAgainClicked -> if (uiState.value.canAskAgain) {
                trackEvent("notification_ask_again_clicked", emptyMap())
                requestPermission()
            }

            NotificationSettingsUiEvent.OnQuietTurnOnClicked -> if (uiState.value.showsQuietNotice) {
                trackEvent("notification_quiet_turn_on_clicked", emptyMap())
                requestPermission()
            }

            is NotificationSettingsUiEvent.OnChannelSettingsClicked -> openChannelSettings(event.type)
        }
    }

    // Why: only while the system silences that row's channel, so a late tap opens nothing.
    private fun openChannelSettings(type: NotificationType) {
        if (!uiState.value.isBlockedBySystem(type)) return
        trackEvent("notification_channel_settings_clicked", mapOf("type" to type))
        emit(NotificationSettingsUiAction.OpenChannelSettings(type))
    }

    // Why: the web and macOS draw it inside the profile, where the back stack doesn't end on this screen.
    private fun leave() {
        if (navigator.backStack.value.lastOrNull() == Route.NotificationSettings) navigator.navigateBack()
    }

    /*
     * Why: Apple's full prompt comes with a push turned on while it delivers quietly; the bell needs no permission. With
     * the choices still loading or the permission unknown (or, for a push, the switch off or the row's channel denied),
     * the toggles are disabled, so a tap that still arrives changes nothing.
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
        // Why: Apple's full prompt while it delivers quietly, for banners and sounds.
        if (isEnabled && channel == NotificationChannel.PUSH && state.showsQuietNotice) requestPermission()
    }

    /*
     * Why: saved on this device whatever the system answers, so the switch is on once it allows them. Turned on, the
     * system is asked while it still can ask (never asked, or Android refused once); refused for good, only its
     * settings can allow them. A tap that changes nothing (the switch already so) is dropped.
     */
    private fun togglePush(isEnabled: Boolean) {
        val state = uiState.value
        if (!state.isPushSwitchEnabled || isEnabled == state.isPushOn) return
        trackEvent("notification_push_toggled", mapOf("is_enabled" to isEnabled))
        // Why: shown at once, so a second tap before Room answers is taken for the same change and dropped.
        uiState.update { it.copy(devicePush = isEnabled.toLoaded()) }
        viewModelScope.launch { updatePushEnabled(isEnabled) }
        if (!isEnabled) return
        when (state.permission) {
            NotificationPermission.NOT_DETERMINED,
            NotificationPermission.REFUSED_ONCE,
            -> requestPermission()

            NotificationPermission.DENIED -> emit(NotificationSettingsUiAction.OpenSystemSettings)

            NotificationPermission.GRANTED,
            NotificationPermission.PROVISIONAL,
            null,
            -> Unit
        }
    }

    private fun requestPermission() {
        awaitsPermissionAnswer = true
        emit(NotificationSettingsUiAction.RequestPermission)
    }

    private fun answerPermission(permission: NotificationPermission) {
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
