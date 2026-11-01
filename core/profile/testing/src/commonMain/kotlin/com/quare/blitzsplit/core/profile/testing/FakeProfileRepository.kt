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
    val hasPendingChanges = MutableStateFlow(false)
    val savedProfiles = mutableListOf<UserProfile>()
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var pushError: Throwable? = null
    var pullError: Throwable? = null

    // The profile the server holds for the user: a successful download writes it to the device.
    var serverProfile: UserProfile? = null
    var clearCount = 0

    override fun observeProfile(): Flow<UserProfile?> = profile

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override suspend fun saveProfile(profile: UserProfile) {
        savedProfiles += profile
        this.profile.value = profile
        hasPendingChanges.value = true
    }

    override suspend fun uploadPendingProfile(userId: String): Result<Unit> {
        uploadedUserIds += userId
        pushError?.let { return Result.failure(it) }
        hasPendingChanges.value = false
        return Result.success(Unit)
    }

    override suspend fun clear() {
        clearCount++
        profile.value = null
        hasPendingChanges.value = false
    }

    override suspend fun downloadProfile(userId: String): Result<Unit> {
        downloadedUserIds += userId
        pullError?.let { return Result.failure(it) }
        serverProfile?.let { profile.value = it }
        return Result.success(Unit)
    }
}
