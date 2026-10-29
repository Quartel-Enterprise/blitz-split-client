package com.quare.blitzsplit.feature.notificationsettings.presentation.model

// Why: each platform reads its own (Android's POST_NOTIFICATIONS, UNUserNotificationCenter, the browser's).
enum class NotificationPermission {
    GRANTED,
    DENIED,

    // Why: never asked yet, so turning a toggle on asks.
    NOT_DETERMINED,
}
