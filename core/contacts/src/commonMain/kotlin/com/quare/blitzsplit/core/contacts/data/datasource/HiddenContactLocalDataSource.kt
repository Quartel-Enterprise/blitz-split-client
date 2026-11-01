package com.quare.blitzsplit.core.contacts.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.HiddenContactEntity
import kotlinx.coroutines.flow.Flow

interface HiddenContactLocalDataSource {
    fun observeAll(): Flow<List<HiddenContactEntity>>

    fun observeHasPending(): Flow<Boolean>

    suspend fun getAll(): List<HiddenContactEntity>

    suspend fun getOrNull(personKey: String): HiddenContactEntity?

    suspend fun upsert(entity: HiddenContactEntity)

    suspend fun markSynced(
        personKey: String,
        syncedUpdatedAt: Long,
    )

    suspend fun applyRemote(entity: HiddenContactEntity)

    suspend fun claimUnowned(ownerUserId: String)

    suspend fun deleteOtherUsersRows(ownerUserId: String)

    suspend fun clear()
}
