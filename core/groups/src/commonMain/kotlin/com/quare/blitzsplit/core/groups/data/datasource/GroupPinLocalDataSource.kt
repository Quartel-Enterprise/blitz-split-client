package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.GroupPinEntity
import kotlinx.coroutines.flow.Flow

interface GroupPinLocalDataSource {
    fun observePinnedGroupIds(): Flow<List<String>>

    fun observeHasPending(): Flow<Boolean>

    suspend fun getPending(): List<GroupPinEntity>

    suspend fun getUpdatedAt(groupId: String): Long?

    suspend fun upsert(entity: GroupPinEntity)

    suspend fun markSynced(
        groupId: String,
        syncedUpdatedAt: Long,
    )

    suspend fun applyRemote(
        groupId: String,
        isPinned: Boolean,
        remoteUpdatedAt: Long,
    )
}
