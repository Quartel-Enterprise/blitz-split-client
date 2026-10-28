package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

interface ProfileLocalDataSource {
    fun observe(): Flow<ProfileEntity?>

    suspend fun get(): ProfileEntity?

    suspend fun upsert(entity: ProfileEntity)

    suspend fun markSynced(
        syncedUpdatedAt: Long,
        photoUrl: String?,
    )

    suspend fun applyRemote(entity: ProfileEntity)

    suspend fun claimUnowned(ownerUserId: String)

    suspend fun replaceOtherUsersCopy(entity: ProfileEntity)

    suspend fun reclaimOtherUsersCopy(
        ownerUserId: String,
        updatedAt: Long,
        photoUrl: String?,
    )

    suspend fun clear()
}
