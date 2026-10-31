package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.ExpenseWithEditor
import kotlinx.coroutines.flow.Flow

interface ExpenseLocalDataSource {
    fun observeExpenses(groupId: String): Flow<List<ExpenseWithEditor>>

    fun observeAllExpenses(): Flow<List<ExpenseWithEditor>>

    // Why: null once the bill is deleted, here or by a sync.
    fun observeExpense(id: String): Flow<ExpenseWithEditor?>

    fun observeHasPending(): Flow<Boolean>

    fun observePendingVersions(): Flow<List<Long>>

    suspend fun getPendingExpenses(ownerUserId: String): List<ExpenseEntity>

    // Why: fails when the group is no longer on the device, which a sync may have removed meanwhile.
    suspend fun insertExpense(entity: ExpenseEntity)

    // Why: what the bill says, from the entity; fails when the bill is gone, which a sync may have deleted meanwhile.
    suspend fun updateExpense(
        entity: ExpenseEntity,
        now: Long,
    )

    // Why: fails when the bill is already gone.
    suspend fun markDeleted(
        id: String,
        now: Long,
    )

    // Why: skips a bill of a group this device doesn't have, which waits for the sync that brings the group.
    suspend fun applyRemote(entities: List<ExpenseEntity>)

    // Why: each bill at the version that went up; one changed meanwhile stays pending.
    suspend fun markSynced(pushed: List<ExpenseEntity>)

    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverExpenseIds: Set<String>,
    )
}
