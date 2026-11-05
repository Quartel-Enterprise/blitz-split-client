package com.quare.blitzsplit.core.profile.domain.repository

import com.quare.blitzsplit.core.profile.domain.model.AppInviteStatus
import kotlinx.coroutines.flow.Flow

interface AppInviteRepository {
    fun observeStatus(): Flow<AppInviteStatus>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: "I downloaded it" and ✕, on the device first, offline too; the next sync takes it to the account.
    suspend fun dismiss()

    // Why: records this platform for the account, then brings its dismissal and phone use down and the device's up.
    suspend fun sync(userId: String): Result<Unit>

    // Why: a sign out leaves nothing of the account on the device.
    suspend fun clear()
}
