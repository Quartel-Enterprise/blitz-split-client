package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.model.StoredBill
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    // Why: the group's bills, the most recent first; a deleted one is gone.
    fun observeExpenses(groupId: String): Flow<List<Expense>>

    // Why: every group's bills at once, each group's in the order of its own list; a deleted one is gone.
    fun observeAllExpenses(): Flow<List<Expense>>

    /*
     * Why: the first `limit` bills this device's user paid or takes part in, from every group, the most recent first
     * (Início's "Ver tudo"); a deleted one is gone.
     */
    fun observeMyExpenses(limit: Int): Flow<List<Expense>>

    // Why: one bill, null once it is deleted, here or on another device.
    fun observeExpense(id: String): Flow<Expense?>

    /*
     * Why: the group of each bill deleted here whose tombstone waits to go up, once per bill: gone from the lists,
     * still a change in the queue.
     */
    fun observePendingDeletionGroupIds(): Flow<List<String>>

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

    /*
     * Why: "Editar conta", on the device first like a new bill. Keeps who created it, when and in which group; fails
     * when the bill is gone.
     */
    suspend fun updateExpense(
        expense: Expense,
        updatedAt: Long,
    ): Result<Unit>

    // Why: "Apagar conta", a tombstone the sync takes up; fails when the bill is already gone.
    suspend fun deleteExpense(
        id: String,
        deletedAt: Long,
    ): Result<Unit>

    suspend fun uploadPendingExpenses(userId: String): Result<Unit>

    // Why: after the groups' download, so a bill of a group that just came down has its group on the device.
    suspend fun downloadExpenses(userId: String): Result<Unit>

    // Why: one bill as the server keeps it, null when the user may not see it (a group they aren't in).
    suspend fun fetchServerBill(id: String): Result<StoredBill?>

    // Why: one bill as the device keeps it, a deletion not up yet or one this version can't read included.
    suspend fun findDeviceBill(id: String): StoredBill?

    // Why: one group's bills for a first download, written by the write it returns; nothing leaves the device.
    suspend fun fetchGroupExpenses(groupId: String): Result<suspend () -> Unit>
}
