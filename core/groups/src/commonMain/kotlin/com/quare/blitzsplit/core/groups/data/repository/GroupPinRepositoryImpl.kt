package com.quare.blitzsplit.core.groups.data.repository

import com.quare.blitzsplit.core.groups.data.datasource.GroupPinLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupPinRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.GroupPinDto
import com.quare.blitzsplit.core.groups.domain.repository.GroupPinRepository
import com.quare.blitzsplit.core.provider.room.entity.GroupPinEntity
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GroupPinRepositoryImpl(
    private val local: GroupPinLocalDataSource,
    private val remote: GroupPinRemoteDataSource,
) : GroupPinRepository {
    override fun observePinnedGroupIds(): Flow<Set<String>> = local
        .observePinnedGroupIds()
        .map { it.toSet() }
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    // Why: MAX(now, updatedAt + 1), so a change wins over the pin it read even with the device's clock behind.
    override suspend fun updatePinned(
        groupId: String,
        isPinned: Boolean,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        val stored = local.getUpdatedAt(groupId)
        local.upsert(
            GroupPinEntity(
                groupId = groupId,
                isPinned = isPinned,
                updatedAt = stored?.let { maxOf(updatedAt, it + 1) } ?: updatedAt,
                pendingSync = true,
            ),
        )
    }

    override suspend fun downloadPins(userId: String): Result<Unit> = suspendRunCatching {
        remote.fetchPins().forEach { pin ->
            local.applyRemote(
                groupId = pin.groupId,
                isPinned = pin.isPinned,
                remoteUpdatedAt = pin.updatedAt,
            )
        }
    }

    override suspend fun uploadPendingPins(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.getPending()
        if (pending.isEmpty()) return@suspendRunCatching
        remote.upsertPins(
            pending.map { pin ->
                GroupPinDto(
                    userId = userId,
                    groupId = pin.groupId,
                    isPinned = pin.isPinned,
                    updatedAt = pin.updatedAt,
                )
            },
        )
        pending.forEach { local.markSynced(it.groupId, it.updatedAt) }
    }
}
