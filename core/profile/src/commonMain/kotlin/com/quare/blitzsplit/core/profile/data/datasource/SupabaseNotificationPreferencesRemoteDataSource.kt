package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.NotificationPreferencesDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseNotificationPreferencesRemoteDataSource(
    private val postgrest: Postgrest,
) : NotificationPreferencesRemoteDataSource {
    override suspend fun upsert(preferences: NotificationPreferencesDto) {
        postgrest.from(TABLE).upsert(preferences)
    }

    override suspend fun fetch(userId: String): NotificationPreferencesDto? = postgrest
        .from(TABLE)
        .select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull<NotificationPreferencesDto>()

    private companion object {
        const val TABLE = "notification_preferences"
    }
}
