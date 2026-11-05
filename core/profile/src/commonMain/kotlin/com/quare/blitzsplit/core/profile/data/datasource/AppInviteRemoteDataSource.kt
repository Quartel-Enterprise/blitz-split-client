package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.AppInviteDto
import com.quare.blitzsplit.core.profile.data.dto.PlatformSeenDto

interface AppInviteRemoteDataSource {
    suspend fun fetchDismissal(userId: String): AppInviteDto?

    // Why: a dismissal is final, so one already there stays as it is.
    suspend fun insertDismissal(dismissal: AppInviteDto)

    suspend fun fetchPlatformsSeen(userId: String): List<PlatformSeenDto>

    suspend fun recordPlatformSeen(platform: String)
}
