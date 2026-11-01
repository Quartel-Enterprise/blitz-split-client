package com.quare.blitzsplit.core.groups.domain.repository

import kotlinx.coroutines.flow.Flow

// Why: "Fixar na Início", this device's user's own pins, synced with the account.
interface GroupPinRepository {
    fun observePinnedGroupIds(): Flow<Set<String>>

    fun observeHasPendingChanges(): Flow<Boolean>

    suspend fun updatePinned(
        groupId: String,
        isPinned: Boolean,
        updatedAt: Long,
    ): Result<Unit>

    // Why: after the groups, since a pin needs its group on the device.
    suspend fun downloadPins(userId: String): Result<Unit>

    // Why: after the groups went up, since the server only lets a user pin a group it has.
    suspend fun uploadPendingPins(userId: String): Result<Unit>
}
