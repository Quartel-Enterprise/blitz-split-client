package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.ExpenseLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.ExpenseRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.ExpenseMapper
import com.quare.blitzsplit.core.expenses.domain.model.Expense
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val local: ExpenseLocalDataSource,
    private val remote: ExpenseRemoteDataSource,
    private val mapper: ExpenseMapper,
) : ExpenseRepository {
    override fun observeExpenses(groupId: String): Flow<List<Expense>> = local
        .observeExpenses(groupId)
        .map { entities -> entities.mapNotNull(mapper::toDomain) }
        .distinctUntilChanged()

    override fun observeExpense(id: String): Flow<Expense?> = local
        .observeExpense(id)
        .map { row -> row?.let(mapper::toDomain) }
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    override fun observePendingVersions(): Flow<List<Long>> = local.observePendingVersions().distinctUntilChanged()

    override suspend fun createExpense(
        expense: Expense,
        createdAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.insertExpense(
            mapper.toEntity(
                expense = expense,
                createdAt = createdAt,
            ),
        )
    }

    override suspend fun updateExpense(
        expense: Expense,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.updateExpense(
            entity = mapper.toEntity(
                expense = expense,
                createdAt = updatedAt,
            ),
            now = updatedAt,
        )
    }

    override suspend fun deleteExpense(
        id: String,
        deletedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.markDeleted(
            id = id,
            now = deletedAt,
        )
    }

    // Why: only the bills of this user's groups, which went up before them in the same sync.
    override suspend fun uploadPendingExpenses(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.getPendingExpenses(userId)
        if (pending.isEmpty()) return@suspendRunCatching
        remote.upsertExpenses(pending.map { mapper.toDto(it, userId) })
        // Why: the server records whoever pushes as who last saved each bill.
        local.markSynced(pending.map { it.copy(updatedBy = userId) })
    }

    override suspend fun downloadExpenses(userId: String): Result<Unit> = suspendRunCatching {
        val remoteExpenses = remote.fetchExpenses()
        local.applyRemote(remoteExpenses.map { mapper.toEntity(it) })
        local.deleteGoneFromServer(
            ownerUserId = userId,
            serverExpenseIds = remoteExpenses.map { it.id }.toSet(),
        )
    }
}
