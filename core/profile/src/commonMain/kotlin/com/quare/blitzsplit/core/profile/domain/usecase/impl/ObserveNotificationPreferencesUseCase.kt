package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.NotificationPreferences
import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObserveNotificationPreferences
import kotlinx.coroutines.flow.Flow

class ObserveNotificationPreferencesUseCase(
    private val repository: NotificationPreferencesRepository,
) : ObserveNotificationPreferences {
    override fun invoke(): Flow<NotificationPreferences> = repository.observePreferences()
}
