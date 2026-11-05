package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.AppInviteEntity
import kotlinx.coroutines.flow.Flow

interface AppInviteLocalDataSource {
    fun observe(): Flow<AppInviteEntity?>

    suspend fun get(): AppInviteEntity?

    suspend fun upsert(entity: AppInviteEntity)

    suspend fun markSynced(syncedDismissedAt: Long)

    suspend fun claimUnowned(ownerUserId: String)

    suspend fun deleteOtherUsers(ownerUserId: String)

    suspend fun clear()
}
