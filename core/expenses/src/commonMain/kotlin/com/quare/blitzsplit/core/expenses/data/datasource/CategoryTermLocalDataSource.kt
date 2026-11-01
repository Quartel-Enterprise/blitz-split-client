package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.CategoryTermEntity
import kotlinx.coroutines.flow.Flow

interface CategoryTermLocalDataSource {
    fun observeAll(): Flow<List<CategoryTermEntity>>

    fun observeHasPending(): Flow<Boolean>

    suspend fun getAll(): List<CategoryTermEntity>

    suspend fun upsert(entities: List<CategoryTermEntity>)

    suspend fun markSynced(
        termKey: String,
        syncedUpdatedAt: Long,
    )

    suspend fun claimUnowned(ownerUserId: String)

    suspend fun deleteOtherUsersTerms(ownerUserId: String)

    suspend fun clear()
}
