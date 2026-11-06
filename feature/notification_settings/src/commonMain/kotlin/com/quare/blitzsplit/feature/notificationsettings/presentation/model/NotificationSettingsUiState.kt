package com.quare.blitzsplit.feature.notificationsettings.presentation.model

import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationPermission
import com.quare.blitzsplit.core.model.NotificationPreferences
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
    val preferences: Loadable<NotificationPreferences>,
    // Why: the "Push notifications" switch of this device, Loading until Room was read.
    val devicePush: Loadable<Boolean>,
    // Why: null until the UI reads the system's: unknown, never taken as allowed nor as denied.
    val permission: NotificationPermission?,
    // Why: Android: the rows whose channel the system silenced; iOS and the web have no switch per row.
    val systemBlockedTypes: Set<NotificationType>,
) {
    // Why: the rows, in the order every UI lists them, each with a Push and, but the weekly reminder, an In app toggle.
    val types: List<NotificationType>
        get() = NotificationType.entries

    // Why: the columns, Push then In app.
    val channels: List<NotificationChannel>
        get() = NotificationChannel.entries

    val isPermissionKnown: Boolean
        get() = permission != null

    // Why: the system lets this device show them, with banners or (Apple) quietly in Notification Center.
    val isPermissionAllowed: Boolean
        get() = permission == NotificationPermission.GRANTED || permission == NotificationPermission.PROVISIONAL

    /*
     * Why: the "Push notifications" switch: on while this device takes pushes and the system allows them, so it shows
     * off until the permission is granted, whatever was saved.
     */
    val isPushOn: Boolean
        get() = isPermissionAllowed && devicePush.valueOrNull() == true

    // Why: the switch shimmers until both its own state and the system's permission are known.
    val showsPushSwitchLoading: Boolean
        get() = devicePush.isLoading() || !isPermissionKnown

    // Why: a tap before both are known couldn't tell whether to ask the system.
    val isPushSwitchEnabled: Boolean
        get() = devicePush.isLoaded() && isPermissionKnown

    // Why: the toggles shimmer until the choices, the switch and the system's permission are known.
    val showsTogglesLoading: Boolean
        get() = preferences.isLoading() || showsPushSwitchLoading

    /*
     * Why: the 4.8b notice, with the button that opens the system settings; Android refused once too, whose button
     * asks again, since Android still shows its dialog once more.
     */
    val showsPermissionDenied: Boolean
        get() = permission == NotificationPermission.DENIED || permission == NotificationPermission.REFUSED_ONCE

    // Why: the notice's "Turn on", which asks the system again, instead of "Open settings".
    val canAskAgain: Boolean
        get() = permission == NotificationPermission.REFUSED_ONCE

    /*
     * Why: Apple delivers quietly to Notification Center: a notice whose "Turn on" brings its full prompt. Only while the
     * switch is on, since with it off nothing reaches this device, quietly or not.
     */
    val showsQuietNotice: Boolean
        get() = permission == NotificationPermission.PROVISIONAL && devicePush.valueOrNull() == true

    /*
     * Why: "Blocked in Android" on a row whose channel the system silenced, while the app's notifications are allowed;
     * with them all denied, the notice above already says so.
     */
    fun isBlockedBySystem(type: NotificationType): Boolean =
        isPermissionKnown && !showsPermissionDenied && type in systemBlockedTypes

    /*
     * Why: a tap before the choices came down would mark the defaults to go up over the server's. With the switch off
     * (turned off, or the permission not granted) every Push shows off and disabled, its choice kept: only the switch
     * asks the system.
     */
    val arePushTogglesEnabled: Boolean
        get() = preferences.isLoaded() && isPushOn

    // Why: the bell needs no permission, so it stays in reach while the system denies the pushes.
    val areInAppTogglesEnabled: Boolean
        get() = preferences.isLoaded() && isPermissionKnown

    fun areTogglesEnabled(channel: NotificationChannel): Boolean = when (channel) {
        NotificationChannel.PUSH -> arePushTogglesEnabled
        NotificationChannel.IN_APP -> areInAppTogglesEnabled
    }

    // Why: a row whose channel the system silenced takes no Push change, which would deliver nothing.
    fun isToggleEnabled(
        type: NotificationType,
        channel: NotificationChannel,
    ): Boolean = areTogglesEnabled(channel) && !(channel == NotificationChannel.PUSH && isBlockedBySystem(type))

    // Why: the weekly reminder never shows in Activity, so its In app cell stays empty.
    fun hasToggle(
        type: NotificationType,
        channel: NotificationChannel,
    ): Boolean = channel == NotificationChannel.PUSH || type.hasInApp

    /*
     * Why: with the switch off, or the system denying the row's channel, a push toggle shows off while the choice stays
     * saved.
     */
    fun isEnabled(
        type: NotificationType,
        channel: NotificationChannel,
    ): Boolean = isToggleEnabled(type, channel) &&
        hasToggle(type, channel) &&
        preferences.valueOrNull()?.isEnabled(type, channel) == true
}
