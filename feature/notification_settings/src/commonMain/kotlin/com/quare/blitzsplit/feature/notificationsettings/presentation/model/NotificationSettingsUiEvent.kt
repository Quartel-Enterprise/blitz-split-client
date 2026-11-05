package com.quare.blitzsplit.feature.notificationsettings.presentation.model

import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface NotificationSettingsUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, or what the system says of each row's channel: nobody tapped to act.
            OnBackClicked,
            is OnSystemBlockedTypesChecked,
            -> EventAnalytics.NotTracked

            // Why: what the system says, which nobody tapped, but the first check after the app asked is its answer.
            is OnPermissionChecked -> EventAnalytics.Manual("notification_permission_answered")

            // Why: only while its channel's toggles take a change (the preferences read, the push's permission not refused).
            is OnToggled -> EventAnalytics.Manual("notification_toggled")

            OnOpenSettingsClicked -> EventAnalytics.Automatic("notification_system_settings_clicked")

            // Why: only while Android can still ask (refused once).
            OnAskAgainClicked -> EventAnalytics.Manual("notification_ask_again_clicked")

            // Why: only while Apple delivers quietly.
            OnQuietTurnOnClicked -> EventAnalytics.Manual("notification_quiet_turn_on_clicked")

            // Why: only while Android silences that row's channel.
            is OnChannelSettingsClicked -> EventAnalytics.Manual("notification_channel_settings_clicked")
        }

    data object OnBackClicked : NotificationSettingsUiEvent

    // Why: read on appear and on return to the app, since the person may change it in the system settings.
    data class OnPermissionChecked(
        val permission: NotificationPermission,
    ) : NotificationSettingsUiEvent

    data class OnToggled(
        val type: NotificationType,
        val channel: NotificationChannel,
        val isEnabled: Boolean,
    ) : NotificationSettingsUiEvent

    data object OnOpenSettingsClicked : NotificationSettingsUiEvent

    // Why: the notice's "Turn on" on Android after a first refusal, which shows the system's dialog once more.
    data object OnAskAgainClicked : NotificationSettingsUiEvent

    // Why: the notice's "Turn on" while Apple delivers quietly, which brings its full prompt for banners and sounds.
    data object OnQuietTurnOnClicked : NotificationSettingsUiEvent

    /*
     * Why: Android only, on appear and on return: the rows whose channel the person silenced in its settings. Its
     * channels are Android's, so it maps them to the rows; iOS and the web have no switch per row and never send it.
     */
    data class OnSystemBlockedTypesChecked(
        val types: Set<NotificationType>,
    ) : NotificationSettingsUiEvent

    // Why: "Open settings" on a row its system silenced, which opens that channel's settings.
    data class OnChannelSettingsClicked(
        val type: NotificationType,
    ) : NotificationSettingsUiEvent
}
