package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// In-memory ExpenseRepository: the state is public so a test can set and read it, every call is
// recorded, and the failure knobs are public vars.
class FakeExpenseRepository(
    initialExpenses: List<Expense> = emptyList(),
) : ExpenseRepository {
    val expenses = MutableStateFlow(initialExpenses)
    val hasPendingChanges = MutableStateFlow(false)
    val pendingVersions = MutableStateFlow(emptyList<Long>())
    val createdExpenses = mutableListOf<Expense>()
    val createdAts = mutableListOf<Long>()
    val updatedExpenses = mutableListOf<Expense>()
    val updatedAts = mutableListOf<Long>()
    val deletedIds = mutableListOf<String>()
    val deletedAts = mutableListOf<Long>()
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var createError: Throwable? = null
    var updateError: Throwable? = null
    var deleteError: Throwable? = null

    // Why: while set, a create waits for it, so a test can act while the save is in flight.
    var createGate: CompletableDeferred<Unit>? = null

    // Why: while set, a delete waits for it, so a test can act while the delete is in flight.
    var deleteGate: CompletableDeferred<Unit>? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null

    // Why: like Room, the group's bills with the most recent first.
    override fun observeExpenses(groupId: String): Flow<List<Expense>> = expenses.map { list ->
        list.filter { it.groupId == groupId }.sortedByDescending { it.spentAt }
    }

    // Why: like Room, each group's bills together, the most recent first.
    override fun observeAllExpenses(): Flow<List<Expense>> = expenses.map { list ->
        list.sortedWith(compareBy<Expense> { it.groupId }.thenByDescending { it.spentAt })
    }

    override fun observeExpense(id: String): Flow<Expense?> = expenses.map { list -> list.firstOrNull { it.id == id } }

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override fun observePendingVersions(): Flow<List<Long>> = pendingVersions

    override suspend fun createExpense(
        expense: Expense,
        createdAt: Long,
    ): Result<Unit> {
        createGate?.await()
        createError?.let { return Result.failure(it) }
        createdExpenses += expense
        createdAts += createdAt
        expenses.update { it + expense }
        return Result.success(Unit)
    }

    // Why: like Room, the bill keeps its group and takes the new version; a bill that is gone fails.
    override suspend fun updateExpense(
        expense: Expense,
        updatedAt: Long,
    ): Result<Unit> {
        updateError?.let { return Result.failure(it) }
        val current = expenses.value.firstOrNull { it.id == expense.id }
            ?: return Result.failure(IllegalStateException("No bill ${expense.id}"))
        updatedExpenses += expense
        updatedAts += updatedAt
        val updated = expense.copy(
            groupId = current.groupId,
            isPending = true,
            updatedAt = maxOf(updatedAt, current.updatedAt + 1),
            updatedByMemberId = current.updatedByMemberId,
        )
        expenses.update { list -> list.map { if (it.id == expense.id) updated else it } }
        return Result.success(Unit)
    }

    override suspend fun deleteExpense(
        id: String,
        deletedAt: Long,
    ): Result<Unit> {
        deleteGate?.await()
        deleteError?.let { return Result.failure(it) }
        if (expenses.value.none { it.id == id }) return Result.failure(IllegalStateException("No bill $id"))
        deletedIds += id
        deletedAts += deletedAt
        expenses.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }

    override suspend fun uploadPendingExpenses(userId: String): Result<Unit> {
        uploadedUserIds += userId
        pushError?.let { return Result.failure(it) }
        hasPendingChanges.value = false
        pendingVersions.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun downloadExpenses(userId: String): Result<Unit> {
        downloadedUserIds += userId
        return pullError?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}
