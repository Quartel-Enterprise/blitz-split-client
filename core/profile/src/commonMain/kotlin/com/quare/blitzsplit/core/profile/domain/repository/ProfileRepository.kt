package com.quare.blitzsplit.core.profile.domain.repository

import com.quare.blitzsplit.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: the code of the account's profile link, null while no account owns this device's profile.
    fun observeShareCode(): Flow<String?>

    suspend fun saveProfile(profile: UserProfile)

    suspend fun uploadPendingProfile(userId: String): Result<Unit>

    /*
     * Why: a copy of another user's profile, after a switch, gives way to this user's, even a change not pushed
     * yet, and goes up as this user's only if it has none on the server.
     */
    suspend fun downloadProfile(userId: String): Result<Unit>

    // Why: a sign out leaves no copy of the profile on the device.
    suspend fun clear()
}
