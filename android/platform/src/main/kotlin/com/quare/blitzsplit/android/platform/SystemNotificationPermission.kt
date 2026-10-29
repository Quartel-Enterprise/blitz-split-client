package com.quare.blitzsplit.android.platform

enum class SystemNotificationPermission {
    GRANTED,

    // Why: off in the system settings or refused, so the app sends the person to the settings to turn it on.
    DENIED,

    // Why: never asked by this app (even if refused outside it), or a dialog dismissed without an answer.
    ASKABLE,
}
