package com.quare.blitzsplit.feature.notificationsettings.presentation.model

import com.quare.blitzsplit.core.model.NotificationType

sealed interface NotificationSettingsUiAction {
    // Why: each platform asks its own way and answers with OnPermissionChecked.
    data object RequestPermission : NotificationSettingsUiAction

    // Why: the app's notification settings on Android, iOS and macOS; the web explains it instead.
    data object OpenSystemSettings : NotificationSettingsUiAction

    // Why: Android only: the settings of the one channel its system silenced, which is the row's own.
    data class OpenChannelSettings(
        val type: NotificationType,
    ) : NotificationSettingsUiAction
}
