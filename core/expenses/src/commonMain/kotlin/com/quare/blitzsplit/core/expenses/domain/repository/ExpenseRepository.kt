package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    // Why: the group's bills, the most recent first; a deleted one is gone.
    fun observeExpenses(groupId: String): Flow<List<Expense>>

    fun observeHasPendingChanges(): Flow<Boolean>

    /*
     * Why: when each bill waiting to go up was last changed. A new bill changes it even while others already wait
     * after a push that failed, so the sync knows to try again.
     */
    fun observePendingVersions(): Flow<List<Long>>

    // Why: on the device first, offline included; the sync takes it to the server after its group and members.
    suspend fun createExpense(
        expense: Expense,
        createdAt: Long,
    ): Result<Unit>

    suspend fun uploadPendingExpenses(userId: String): Result<Unit>

    // Why: after the groups' download, so a bill of a group that just came down has its group on the device.
    suspend fun downloadExpenses(userId: String): Result<Unit>
}
