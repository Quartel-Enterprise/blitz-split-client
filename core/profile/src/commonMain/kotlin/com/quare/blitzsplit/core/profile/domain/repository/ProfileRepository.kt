package com.quare.blitzsplit.core.profile.domain.repository

import com.quare.blitzsplit.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile?>

    fun observeHasPendingChanges(): Flow<Boolean>

    suspend fun saveProfile(profile: UserProfile)

    suspend fun uploadPendingProfile(userId: String): Result<Unit>

    suspend fun downloadProfile(userId: String): Result<Unit>

    /*
     * Why: on a switch to another user the copy on the device was the previous user's, so the account's profile
     * replaces it on the next pull, even over a change not pushed yet. It goes up only if the account has none.
     */
    suspend fun deferToAccountProfile()

    // Why: a sign out leaves no copy of the profile on the device.
    suspend fun clear()
}
