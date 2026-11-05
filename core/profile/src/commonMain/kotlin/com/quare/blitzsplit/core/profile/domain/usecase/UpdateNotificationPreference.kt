package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.NotificationChannel
import com.quare.blitzsplit.core.model.NotificationType

fun interface UpdateNotificationPreference {
    suspend operator fun invoke(
        type: NotificationType,
        channel: NotificationChannel,
        isEnabled: Boolean,
    )
}
