package com.quare.blitzsplit.core.profile.domain.repository

import com.quare.blitzsplit.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>

    suspend fun saveProfile(profile: UserProfile)

    suspend fun uploadPendingProfile(userId: String): Result<Unit>

    suspend fun downloadProfile(userId: String): Result<Unit>
}
