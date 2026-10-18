package com.quare.blitzsplit.core.profile.testing

import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// In-memory ProfileRepository: the state is public so a test can set and read it, every call is
// recorded, and the failure knobs are public vars.
class FakeProfileRepository(
    initialProfile: UserProfile? = null,
) : ProfileRepository {
    val profile = MutableStateFlow(initialProfile)
    val savedProfiles = mutableListOf<UserProfile>()
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var pushError: Throwable? = null
    var pullError: Throwable? = null

    override fun observeProfile(): Flow<UserProfile?> = profile

    override suspend fun saveProfile(profile: UserProfile) {
        savedProfiles += profile
        this.profile.value = profile
    }

    override suspend fun uploadPendingProfile(userId: String): Result<Unit> {
        uploadedUserIds += userId
        return pushError?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun downloadProfile(userId: String): Result<Unit> {
        downloadedUserIds += userId
        return pullError?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}
