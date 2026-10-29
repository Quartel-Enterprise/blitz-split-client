package com.quare.blitzsplit.feature.notificationsettings.presentation.model

sealed interface NotificationSettingsUiAction {
    // Why: each platform asks its own way and answers with OnPermissionChecked.
    data object RequestPermission : NotificationSettingsUiAction

    // Why: the app's notification settings on Android, iOS and macOS; the web explains it instead.
    data object OpenSystemSettings : NotificationSettingsUiAction
}
