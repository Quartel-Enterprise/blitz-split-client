package com.quare.blitzsplit.core.profile.domain.usecase.impl

import com.quare.blitzsplit.core.model.NotificationType
import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.domain.usecase.UpdateNotificationPreference

class UpdateNotificationPreferenceUseCase(
    private val repository: NotificationPreferencesRepository,
) : UpdateNotificationPreference {
    override suspend fun invoke(
        type: NotificationType,
        isEnabled: Boolean,
    ) {
        repository.updatePreference(
            type = type,
            isEnabled = isEnabled,
        )
    }
}
