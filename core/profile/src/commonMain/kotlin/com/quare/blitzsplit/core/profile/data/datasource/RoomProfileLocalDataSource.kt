package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.ProfileDao
import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

class RoomProfileLocalDataSource(
    private val dao: ProfileDao,
) : ProfileLocalDataSource {
    override fun observe(): Flow<ProfileEntity?> = dao.observe()

    override suspend fun get(): ProfileEntity? = dao.get()

    override suspend fun upsert(entity: ProfileEntity) {
        dao.upsert(entity)
    }

    override suspend fun markSynced(syncedUpdatedAt: Long) {
        dao.markSynced(syncedUpdatedAt)
    }

    override suspend fun applyRemote(entity: ProfileEntity) {
        if (dao.get() == null) {
            dao.upsert(entity)
        } else {
            dao.applyRemote(
                displayName = entity.displayName,
                avatarColor = entity.avatarColor,
                photoUrl = entity.photoUrl,
                remoteUpdatedAt = entity.updatedAt,
            )
        }
    }
}
