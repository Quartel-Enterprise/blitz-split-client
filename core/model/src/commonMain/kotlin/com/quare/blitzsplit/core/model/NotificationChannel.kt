package com.quare.blitzsplit.core.model

// Why: how each row of Notifications reaches the person, each turned on or off on its own.
enum class NotificationChannel {
    // Why: the system's notification, from the server.
    PUSH,

    // Why: the bell: Activity's blue dot and the badge. Off, the events still show in Activity, read already.
    IN_APP,
}
