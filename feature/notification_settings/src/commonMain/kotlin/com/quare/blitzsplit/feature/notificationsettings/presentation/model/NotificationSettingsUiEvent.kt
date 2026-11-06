package com.quare.blitzsplit.feature.notificationsettings.presentation.model

import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface NotificationSettingsUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, or what the system says of the permission or of each row's channel: nobody tapped to act.
            OnBackClicked,
            is OnPermissionChecked,
            is OnSystemBlockedTypesChecked,
            -> EventAnalytics.NotTracked

            // Why: what the system says, which nobody tapped, but it ends the funnel the app's request began.
            is OnPermissionAnswered -> EventAnalytics.Manual("notification_permission_answered")

            // Why: only while its channel's toggles take a change (the preferences read, the pushes on on this device).
            is OnToggled -> EventAnalytics.Manual("notification_toggled")

            // Why: only once the switch and the permission are known, and only a tap that changes the switch.
            is OnPushToggled -> EventAnalytics.Manual("notification_push_toggled")

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

    /*
     * Why: the permission read right after the system's prompt the app asked for (RequestPermission), apart from the
     * reads on appear and return, so one already under way when the request went out is never taken for its answer.
     */
    data class OnPermissionAnswered(
        val permission: NotificationPermission,
    ) : NotificationSettingsUiEvent

    data class OnToggled(
        val type: NotificationType,
        val channel: NotificationChannel,
        val isEnabled: Boolean,
    ) : NotificationSettingsUiEvent

    /*
     * Why: the "Push notifications" switch over the rows, for this device only: on, it asks the system when it still
     * can, or opens its settings when only they can allow them.
     */
    data class OnPushToggled(
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
