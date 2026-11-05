package com.quare.blitzsplit.core.model

// Why: synced with the account, so every device shows the same toggles; the OS permission stays per device.
data class NotificationPreferences(
    val push: Set<NotificationType>,
    // Why: only types whose hasInApp is true.
    val inApp: Set<NotificationType>,
) {
    fun isEnabled(
        type: NotificationType,
        channel: NotificationChannel,
    ): Boolean = type in typesOf(channel)

    fun typesOf(channel: NotificationChannel): Set<NotificationType> = when (channel) {
        NotificationChannel.PUSH -> push
        NotificationChannel.IN_APP -> inApp
    }

    fun with(
        type: NotificationType,
        channel: NotificationChannel,
        isEnabled: Boolean,
    ): NotificationPreferences {
        val current = typesOf(channel)
        val types = if (isEnabled) current + type else current - type
        return when (channel) {
            NotificationChannel.PUSH -> copy(push = types)
            NotificationChannel.IN_APP -> copy(inApp = types.filterTo(mutableSetOf()) { it.hasInApp })
        }
    }

    companion object {
        // Why: what a user who never changed a toggle gets, on the device and for the server alike.
        val DEFAULT: NotificationPreferences = NotificationPreferences(
            push = NotificationType.entries.toSet() - NotificationType.PEOPLE_AND_GROUPS,
            inApp = NotificationType.entries.filterTo(mutableSetOf()) { it.hasInApp },
        )
    }
}
