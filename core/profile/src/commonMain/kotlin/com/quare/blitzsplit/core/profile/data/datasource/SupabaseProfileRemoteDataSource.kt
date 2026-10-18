package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.ProfileDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseProfileRemoteDataSource(
    private val postgrest: Postgrest,
) : ProfileRemoteDataSource {
    override suspend fun upsert(profile: ProfileDto) {
        postgrest.from(TABLE).upsert(profile)
    }

    override suspend fun fetch(userId: String): ProfileDto? = postgrest
        .from(TABLE)
        .select {
            filter { eq("id", userId) }
        }.decodeSingleOrNull<ProfileDto>()

    private companion object {
        const val TABLE = "profiles"
    }
}
