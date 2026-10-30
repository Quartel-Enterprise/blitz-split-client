package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.ExpenseDao
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class RoomExpenseLocalDataSource(
    private val dao: ExpenseDao,
) : ExpenseLocalDataSource {
    override fun observeExpenses(groupId: String): Flow<List<ExpenseEntity>> = dao.observeExpenses(groupId)

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override fun observePendingVersions(): Flow<List<Long>> = dao.observePendingUpdatedAts()

    override suspend fun getPendingExpenses(ownerUserId: String): List<ExpenseEntity> =
        dao.getPendingExpenses(ownerUserId)

    override suspend fun insertExpense(entity: ExpenseEntity) {
        dao.insertExpenseIntoGroup(entity)
    }

    override suspend fun applyRemote(entities: List<ExpenseEntity>) {
        dao.applyRemoteToGroups(entities)
    }

    override suspend fun markSynced(pushed: List<ExpenseEntity>) {
        dao.markAllSynced(pushed)
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverExpenseIds: Set<String>,
    ) {
        dao.deleteGoneFromServer(
            ownerUserId = ownerUserId,
            serverExpenseIds = serverExpenseIds,
        )
    }
}
