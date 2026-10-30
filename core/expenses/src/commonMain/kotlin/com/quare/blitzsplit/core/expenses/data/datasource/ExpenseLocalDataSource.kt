package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

interface ExpenseLocalDataSource {
    fun observeExpenses(groupId: String): Flow<List<ExpenseEntity>>

    fun observeHasPending(): Flow<Boolean>

    fun observePendingVersions(): Flow<List<Long>>

    suspend fun getPendingExpenses(ownerUserId: String): List<ExpenseEntity>

    // Why: fails when the group is no longer on the device, which a sync may have removed meanwhile.
    suspend fun insertExpense(entity: ExpenseEntity)

    // Why: skips a bill of a group this device doesn't have, which waits for the sync that brings the group.
    suspend fun applyRemote(entities: List<ExpenseEntity>)

    suspend fun markSynced(
        id: String,
        syncedUpdatedAt: Long,
    )

    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverExpenseIds: Set<String>,
    )
}
