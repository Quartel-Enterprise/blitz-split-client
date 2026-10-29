package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.NotificationPreferencesDto

interface NotificationPreferencesRemoteDataSource {
    suspend fun upsert(preferences: NotificationPreferencesDto)

    // Why: null for a user who never changed a toggle, who has the defaults.
    suspend fun fetch(userId: String): NotificationPreferencesDto?
}
