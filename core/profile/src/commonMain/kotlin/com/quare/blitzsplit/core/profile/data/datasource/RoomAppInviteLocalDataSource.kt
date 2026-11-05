package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.AppInviteDao
import com.quare.blitzsplit.core.provider.room.entity.AppInviteEntity
import kotlinx.coroutines.flow.Flow

class RoomAppInviteLocalDataSource(
    private val dao: AppInviteDao,
) : AppInviteLocalDataSource {
    override fun observe(): Flow<AppInviteEntity?> = dao.observe()

    override suspend fun get(): AppInviteEntity? = dao.get()

    override suspend fun upsert(entity: AppInviteEntity) {
        dao.upsert(entity)
    }

    override suspend fun markSynced(syncedDismissedAt: Long) {
        dao.markSynced(syncedDismissedAt)
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        dao.claimUnowned(ownerUserId)
    }

    override suspend fun deleteOtherUsers(ownerUserId: String) {
        dao.deleteOtherUsers(ownerUserId)
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}
