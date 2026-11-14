package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.BillPayingMethodEntity
import com.quare.blitzsplit.core.provider.room.entity.PayingMethodEntity
import kotlinx.coroutines.flow.Flow

interface PayingMethodLocalDataSource {
    fun observeLiveMethods(): Flow<List<PayingMethodEntity>>

    fun observeMarks(): Flow<List<BillPayingMethodEntity>>

    fun observeHasPending(): Flow<Boolean>

    suspend fun getAllMethods(): List<PayingMethodEntity>

    suspend fun getAllMarks(): List<BillPayingMethodEntity>

    suspend fun getMarksReadyToUpload(): List<BillPayingMethodEntity>

    suspend fun upsertMethods(entities: List<PayingMethodEntity>)

    suspend fun upsertMarks(entities: List<BillPayingMethodEntity>)

    suspend fun markMethodSynced(
        id: String,
        syncedUpdatedAt: Long,
    )

    suspend fun markMarkSynced(
        expenseId: String,
        syncedUpdatedAt: Long,
    )

    suspend fun deleteSyncedTombstones()

    suspend fun deleteOrphanMarks()

    // Why: drops another user's methods and marks and claims the ones made before any account.
    suspend fun keepOnlyUser(userId: String)

    suspend fun clear()
}
