package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.profile.data.dto.ProfileDto

interface ProfileRemoteDataSource {
    suspend fun upsert(profile: ProfileDto)

    suspend fun fetch(userId: String): ProfileDto?
}
