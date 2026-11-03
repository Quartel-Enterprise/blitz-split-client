package com.quare.blitzsplit.feature.notificationsettings.presentation.model

import com.quare.blitzsplit.core.model.NotificationType
import software.quare.loadable.Loadable
import software.quare.loadable.isLoaded
import software.quare.loadable.isLoading
import software.quare.loadable.valueOrNull

data class NotificationSettingsUiState(
    /*
     * Why: Loading until Room was read and while a restored account's copy is not whole, since its choices only
     * come with that sync: a missing row reads as the defaults, which would show (and save) over the server's.
     */
    val enabledTypes: Loadable<Set<NotificationType>>,
    // Why: null until the UI reads the system's: unknown, never taken as allowed nor as denied.
    val permission: NotificationPermission?,
) {
    // Why: "Avisar quando", in the order every UI lists them.
    val types: List<NotificationType>
        get() = NotificationType.entries

    val isPermissionKnown: Boolean
        get() = permission != null

    // Why: the toggles shimmer until both the choices and the system's permission are known.
    val showsTogglesLoading: Boolean
        get() = enabledTypes.isLoading() || !isPermissionKnown

    // Why: the 4.8b notice, with the button that opens the system settings.
    val showsPermissionDenied: Boolean
        get() = permission == NotificationPermission.DENIED

    /*
     * Why: a tap before the choices came down would mark the defaults to go up over the server's, and one before
     * the permission is known couldn't tell whether to ask the system.
     */
    val areTogglesEnabled: Boolean
        get() = enabledTypes.isLoaded() && isPermissionKnown && !showsPermissionDenied

    // Why: with the system denying them, every toggle shows off while the choices stay saved for later.
    fun isEnabled(type: NotificationType): Boolean =
        areTogglesEnabled && enabledTypes.valueOrNull()?.contains(type) == true
}
