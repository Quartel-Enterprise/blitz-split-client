package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.CustomColorEntity
import kotlinx.coroutines.flow.Flow

interface CustomColorLocalDataSource {
    fun observeAll(): Flow<List<CustomColorEntity>>

    fun observeHasPending(): Flow<Boolean>

    suspend fun getAll(): List<CustomColorEntity>

    suspend fun getByColor(color: String): CustomColorEntity?

    suspend fun getPending(userId: String): List<CustomColorEntity>

    suspend fun upsert(entities: List<CustomColorEntity>)

    suspend fun markSynced(
        color: String,
        syncedUpdatedAt: Long,
    )

    suspend fun claimUnowned(ownerUserId: String)

    suspend fun deleteOtherUsersColors(ownerUserId: String)

    suspend fun clear()
}
