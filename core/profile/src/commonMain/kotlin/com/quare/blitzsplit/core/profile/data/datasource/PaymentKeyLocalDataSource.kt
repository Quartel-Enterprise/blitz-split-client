package com.quare.blitzsplit.core.profile.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.PaymentKeyEntity
import kotlinx.coroutines.flow.Flow

interface PaymentKeyLocalDataSource {
    fun observeLive(): Flow<List<PaymentKeyEntity>>

    fun observeHasPending(): Flow<Boolean>

    suspend fun getAll(): List<PaymentKeyEntity>

    suspend fun upsert(entities: List<PaymentKeyEntity>)

    suspend fun markSynced(
        id: String,
        syncedUpdatedAt: Long,
    )

    suspend fun deleteSyncedTombstones()

    suspend fun applyRemote(entity: PaymentKeyEntity)

    suspend fun claimUnowned(ownerUserId: String)

    suspend fun deleteOtherUsersKeys(ownerUserId: String)

    suspend fun clear()
}
