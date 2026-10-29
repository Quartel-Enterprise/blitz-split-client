package com.quare.blitzsplit.android.platform

enum class SystemNotificationPermission {
    GRANTED,

    // Why: off in the system settings, or refused for good, so only the settings can turn it on.
    DENIED,

    // Why: never asked, or refused once, which Android 13+ still lets the app ask again.
    ASKABLE,
}
