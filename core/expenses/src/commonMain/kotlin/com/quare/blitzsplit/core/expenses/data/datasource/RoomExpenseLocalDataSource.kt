package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.ExpenseDao
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.ExpenseWithEditor
import kotlinx.coroutines.flow.Flow

class RoomExpenseLocalDataSource(
    private val dao: ExpenseDao,
) : ExpenseLocalDataSource {
    override fun observeExpenses(groupId: String): Flow<List<ExpenseWithEditor>> = dao.observeExpenses(groupId)

    override fun observeAllExpenses(): Flow<List<ExpenseWithEditor>> = dao.observeAllExpenses()

    override fun observeExpense(id: String): Flow<ExpenseWithEditor?> = dao.observeExpense(id)

    override fun observePendingDeletionGroupIds(): Flow<List<String>> = dao.observePendingDeletionGroupIds()

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override fun observePendingVersions(): Flow<List<Long>> = dao.observePendingUpdatedAts()

    override suspend fun getPendingExpenses(ownerUserId: String): List<ExpenseEntity> =
        dao.getPendingExpenses(ownerUserId)

    override suspend fun insertExpense(entity: ExpenseEntity) {
        dao.insertExpenseIntoGroup(entity)
    }

    override suspend fun updateExpense(
        entity: ExpenseEntity,
        now: Long,
    ) {
        val changed = dao.updateExpense(
            id = entity.id,
            description = entity.description,
            amountMinor = entity.amountMinor,
            splitMode = entity.splitMode,
            payersJson = entity.payersJson,
            sharesJson = entity.sharesJson,
            itemsJson = entity.itemsJson,
            spentAt = entity.spentAt,
            now = now,
        )
        check(changed == 1) { "No bill ${entity.id}" }
    }

    override suspend fun markDeleted(
        id: String,
        now: Long,
    ) {
        val changed = dao.markDeleted(
            id = id,
            now = now,
        )
        check(changed == 1) { "No bill $id" }
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
