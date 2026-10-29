package com.quare.blitzsplit.feature.notificationsettings.presentation.model

import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.presentation.UiEvent

sealed interface NotificationSettingsUiEvent : UiEvent {
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
