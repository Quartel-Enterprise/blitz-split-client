package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.NotificationPreferencesEntity
import kotlinx.coroutines.flow.Flow

interface NotificationPreferencesLocalDataSource {
    fun observe(): Flow<NotificationPreferencesEntity?>

    suspend fun get(): NotificationPreferencesEntity?

    suspend fun upsert(entity: NotificationPreferencesEntity)

    suspend fun markSynced(syncedUpdatedAt: Long)

    suspend fun applyRemote(entity: NotificationPreferencesEntity)

    suspend fun claimUnowned(ownerUserId: String)

    suspend fun deleteOtherUsers(ownerUserId: String)

    suspend fun clear()
}
