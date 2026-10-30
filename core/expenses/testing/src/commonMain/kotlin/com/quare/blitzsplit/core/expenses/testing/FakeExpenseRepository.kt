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
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var createError: Throwable? = null

    // Why: while set, a create waits for it, so a test can act while the save is in flight.
    var createGate: CompletableDeferred<Unit>? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null

    // Why: like Room, the group's bills with the most recent first.
    override fun observeExpenses(groupId: String): Flow<List<Expense>> = expenses.map { list ->
        list.filter { it.groupId == groupId }.sortedByDescending { it.spentAt }
    }

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
