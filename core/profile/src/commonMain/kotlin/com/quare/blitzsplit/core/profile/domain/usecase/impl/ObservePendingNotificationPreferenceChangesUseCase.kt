package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.domain.usecase.ObservePendingNotificationPreferenceChanges
import kotlinx.coroutines.flow.Flow

class ObservePendingNotificationPreferenceChangesUseCase(
    private val repository: NotificationPreferencesRepository,
) : ObservePendingNotificationPreferenceChanges {
    override fun invoke(): Flow<Boolean> = repository.observeHasPendingChanges()
}
