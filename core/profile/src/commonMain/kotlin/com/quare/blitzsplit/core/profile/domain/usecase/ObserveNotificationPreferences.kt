package com.quare.blitzsplit.core.profile.domain.usecase

import com.quare.blitzsplit.core.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow

fun interface ObserveNotificationPreferences {
    operator fun invoke(): Flow<NotificationPreferences>
}
