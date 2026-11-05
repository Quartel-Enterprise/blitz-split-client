package com.quare.blitzsplit.core.model

// Why: each platform reads its own (Android's POST_NOTIFICATIONS, UNUserNotificationCenter, the browser's).
enum class NotificationPermission {
    GRANTED,

    // Why: only the system settings can allow them now, so the app sends there (or, on the web, says where).
    DENIED,

    // Why: never asked yet, so the system can still ask.
    NOT_DETERMINED,

    // Why: Android only: refused once, and the system still asks once more before only its settings can allow them.
    REFUSED_ONCE,
}
