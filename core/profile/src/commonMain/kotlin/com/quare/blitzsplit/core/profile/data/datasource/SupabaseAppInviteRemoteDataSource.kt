package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.AppInviteDto
import com.quare.blitzsplit.core.profile.data.dto.PlatformSeenDto
import com.quare.blitzsplit.core.profile.data.dto.RecordPlatformSeenParamsDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc

class SupabaseAppInviteRemoteDataSource(
    private val postgrest: Postgrest,
) : AppInviteRemoteDataSource {
    override suspend fun fetchDismissal(userId: String): AppInviteDto? = postgrest
        .from(INVITES)
        .select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull<AppInviteDto>()

    override suspend fun insertDismissal(dismissal: AppInviteDto) {
        postgrest.from(INVITES).upsert(dismissal) {
            onConflict = "user_id"
            ignoreDuplicates = true
        }
    }

    override suspend fun fetchPlatformsSeen(userId: String): List<PlatformSeenDto> = postgrest
        .from(PLATFORMS_SEEN)
        .select {
            filter { eq("user_id", userId) }
        }.decodeList<PlatformSeenDto>()

    override suspend fun recordPlatformSeen(platform: String) {
        postgrest.rpc(
            function = "record_platform_seen",
            parameters = RecordPlatformSeenParamsDto(platform),
        )
    }

    private companion object {
        const val INVITES = "app_invites"
        const val PLATFORMS_SEEN = "platforms_seen"
    }
}
