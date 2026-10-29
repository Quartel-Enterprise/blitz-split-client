package com.quare.blitzsplit.core.model

// Why: synced with the account, so every device shows the same toggles; the OS permission stays per device.
data class NotificationPreferences(
    val enabledTypes: Set<NotificationType>,
) {
    companion object {
        // Why: what a user who never changed a toggle gets, on the device and for the server alike.
        val DEFAULT: NotificationPreferences = NotificationPreferences(
            enabledTypes = setOf(
                NotificationType.NEW_EXPENSE,
                NotificationType.PAYMENT_MARKED,
                NotificationType.SETTLE_REMINDERS,
            ),
        )
    }
}
