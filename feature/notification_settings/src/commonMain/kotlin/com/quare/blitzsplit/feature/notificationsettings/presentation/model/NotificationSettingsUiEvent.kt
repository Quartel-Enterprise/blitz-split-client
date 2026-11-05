package com.quare.blitzsplit.feature.notificationsettings.presentation.model

import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.presentation.UiEvent
import com.quare.blitzsplit.core.presentation.analytics.EventAnalytics

sealed interface NotificationSettingsUiEvent : UiEvent {
    override val analytics: EventAnalytics
        get() = when (this) {
            // Why: a back, or what the system says about the permission: nobody tapped to act.
            OnBackClicked,
            is OnPermissionChecked,
            -> EventAnalytics.NotTracked

            // Why: only while the toggles take a change (the preferences read, the permission not refused).
            is OnToggled -> EventAnalytics.Manual("notification_toggled")

            OnOpenSettingsClicked -> EventAnalytics.Automatic("notification_system_settings_clicked")
        }

    data object OnBackClicked : NotificationSettingsUiEvent

    // Why: read on appear and on return to the app, since the person may change it in the system settings.
    data class OnPermissionChecked(
        val permission: NotificationPermission,
    ) : NotificationSettingsUiEvent

    data class OnToggled(
        val type: NotificationType,
        val isEnabled: Boolean,
    ) : NotificationSettingsUiEvent

    data object OnOpenSettingsClicked : NotificationSettingsUiEvent
}
