package com.quare.blitzsplit.core.groups.testing

import com.quare.blitzsplit.core.groups.domain.repository.GroupPinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// In-memory GroupPinRepository: the pins are public so a test can set and read them, and every sync call is recorded.
class FakeGroupPinRepository(
    initialPinnedGroupIds: Set<String> = emptySet(),
) : GroupPinRepository {
    val pinnedGroupIds = MutableStateFlow(initialPinnedGroupIds)
    val hasPendingChanges = MutableStateFlow(false)
    val downloadedUserIds = mutableListOf<String>()
    val uploadedUserIds = mutableListOf<String>()
    val pinUpdates = mutableListOf<Triple<String, Boolean, Long>>()
    var updatePinnedError: Throwable? = null
    var downloadError: Throwable? = null
    var uploadError: Throwable? = null

    override fun observePinnedGroupIds(): Flow<Set<String>> = pinnedGroupIds

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override suspend fun updatePinned(
        groupId: String,
        isPinned: Boolean,
        updatedAt: Long,
    ): Result<Unit> {
        pinUpdates += Triple(groupId, isPinned, updatedAt)
        updatePinnedError?.let { return Result.failure(it) }
        pinnedGroupIds.update { if (isPinned) it + groupId else it - groupId }
        hasPendingChanges.value = true
        return Result.success(Unit)
    }

    override suspend fun downloadPins(userId: String): Result<Unit> {
        downloadError?.let { return Result.failure(it) }
        downloadedUserIds += userId
        return Result.success(Unit)
    }

    override suspend fun uploadPendingPins(userId: String): Result<Unit> {
        uploadError?.let { return Result.failure(it) }
        uploadedUserIds += userId
        hasPendingChanges.value = false
        return Result.success(Unit)
    }
}
