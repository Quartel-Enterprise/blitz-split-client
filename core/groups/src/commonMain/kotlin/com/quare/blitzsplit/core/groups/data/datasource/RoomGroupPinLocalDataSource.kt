package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.GroupPinDao
import com.quare.blitzsplit.core.provider.room.entity.GroupPinEntity
import kotlinx.coroutines.flow.Flow

class RoomGroupPinLocalDataSource(
    private val dao: GroupPinDao,
) : GroupPinLocalDataSource {
    override fun observePinnedGroupIds(): Flow<List<String>> = dao.observePinnedGroupIds()

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override suspend fun getPending(): List<GroupPinEntity> = dao.getPending()

    override suspend fun getUpdatedAt(groupId: String): Long? = dao.getUpdatedAt(groupId)

    override suspend fun upsert(entity: GroupPinEntity) {
        dao.upsert(entity)
    }

    override suspend fun markSynced(
        groupId: String,
        syncedUpdatedAt: Long,
    ) {
        dao.markSynced(groupId, syncedUpdatedAt)
    }

    override suspend fun applyRemote(
        groupId: String,
        isPinned: Boolean,
        remoteUpdatedAt: Long,
    ) {
        dao.applyRemote(groupId, isPinned, remoteUpdatedAt)
    }
}
