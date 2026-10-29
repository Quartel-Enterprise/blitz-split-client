package com.quare.blitzsplit.feature.notificationsettings.presentation.model

import com.quare.blitzsplit.core.model.NotificationType

data class NotificationSettingsUiState(
    val enabledTypes: Set<NotificationType>,
    // Why: null until the UI reads the system's, which says nothing denied in the meantime.
    val permission: NotificationPermission?,
) {
    // Why: "Avisar quando", in the order every UI lists them.
    val types: List<NotificationType>
        get() = NotificationType.entries

    // Why: the 4.8b notice, with the button that opens the system settings.
    val showsPermissionDenied: Boolean
        get() = permission == NotificationPermission.DENIED

    val areTogglesEnabled: Boolean
        get() = !showsPermissionDenied

    // Why: with the system denying them, every toggle shows off while the choices stay saved for later.
    fun isEnabled(type: NotificationType): Boolean = areTogglesEnabled && type in enabledTypes
}
