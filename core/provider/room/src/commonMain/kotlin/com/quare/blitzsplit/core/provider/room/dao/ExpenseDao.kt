package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

// Why: below SQLite's oldest cap on bound parameters (999), which some devices still have.
private const val MAX_IDS_PER_QUERY = 500

@Dao
interface ExpenseDao {
    // Why: the most recent bill first, as the group's list shows them.
    @Query("SELECT * FROM expense WHERE groupId = :groupId AND isDeleted = 0 ORDER BY spentAt DESC, addedAt DESC, id")
    fun observeExpenses(groupId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM expense WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT updatedAt FROM expense WHERE pendingSync = 1 ORDER BY updatedAt, id")
    fun observePendingUpdatedAts(): Flow<List<Long>>

    @Query(
        "SELECT * FROM expense WHERE pendingSync = 1 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getPendingExpenses(ownerUserId: String): List<ExpenseEntity>

    @Query(
        "SELECT id FROM expense WHERE pendingSync = 0 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getSyncedExpenseIds(ownerUserId: String): List<String>

    /*
     * Why: a group a sync deleted in the meantime takes no bill. The bill would stay pending with no group to go up
     * with, and keep the sync running round after round.
     */
    @Transaction
    suspend fun insertExpenseIntoGroup(entity: ExpenseEntity) {
        check(hasGroup(entity.groupId)) { "No group ${entity.groupId}" }
        insertExpense(entity)
    }

    @Query("SELECT EXISTS(SELECT 1 FROM split_group WHERE id = :id)")
    suspend fun hasGroup(id: String): Boolean

    @Insert
    suspend fun insertExpense(entity: ExpenseEntity)

    // Why: a bill only the server has comes down whole; one the device has follows Last-Write-Wins, in one write.
    @Transaction
    suspend fun applyRemote(entity: ExpenseEntity) {
        insertExpenseIfAbsent(entity)
        updateFromRemote(
            id = entity.id,
            createdBy = entity.createdBy,
            description = entity.description,
            amountMinor = entity.amountMinor,
            splitMode = entity.splitMode,
            payersJson = entity.payersJson,
            sharesJson = entity.sharesJson,
            spentAt = entity.spentAt,
            isDeleted = entity.isDeleted,
            addedAt = entity.addedAt,
            remoteUpdatedAt = entity.updatedAt,
        )
    }

    /*
     * Why: a sync's whole answer in one transaction rather than one per bill. A bill of a group this device doesn't
     * have waits for the sync that brings the group.
     */
    @Transaction
    suspend fun applyRemoteToGroups(entities: List<ExpenseEntity>) {
        entities.filter { hasGroup(it.groupId) }.forEach { applyRemote(it) }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpenseIfAbsent(entity: ExpenseEntity)

    /*
     * Why: Last-Write-Wins as the server keeps it. Any member edits the same bill, so a newer server row wins even
     * over a change made offline, which the server would refuse as older anyway. A bill never moves to another group.
     */
    @Query(
        "UPDATE expense SET createdBy = :createdBy, description = :description, amountMinor = :amountMinor, " +
            "splitMode = :splitMode, payersJson = :payersJson, sharesJson = :sharesJson, spentAt = :spentAt, " +
            "isDeleted = :isDeleted, addedAt = :addedAt, updatedAt = :remoteUpdatedAt, pendingSync = 0 " +
            "WHERE id = :id AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun updateFromRemote(
        id: String,
        createdBy: String?,
        description: String,
        amountMinor: Long,
        splitMode: String,
        payersJson: String,
        sharesJson: String,
        spentAt: Long,
        isDeleted: Boolean,
        addedAt: Long,
        remoteUpdatedAt: Long,
    )

    // Why: every bill a push took up in one transaction, so the observers of the table wake once rather than per bill.
    @Transaction
    suspend fun markAllSynced(pushed: List<ExpenseEntity>) {
        pushed.forEach {
            markSynced(
                id = it.id,
                syncedUpdatedAt = it.updatedAt,
            )
        }
    }

    // Why: a change made while the push was in flight keeps the bill pending.
    @Query("UPDATE expense SET pendingSync = 0 WHERE id = :id AND updatedAt = :syncedUpdatedAt")
    suspend fun markSynced(
        id: String,
        syncedUpdatedAt: Long,
    )

    /*
     * Why: the server answers every bill of every group the user may see, so a synced bill missing from the answer
     * was removed there. A bill edited here stays, and the bills of a group gone from the server go with the group.
     */
    @Transaction
    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverExpenseIds: Set<String>,
    ) {
        getSyncedExpenseIds(ownerUserId)
            .filterNot { it in serverExpenseIds }
            .chunked(MAX_IDS_PER_QUERY)
            .forEach { deleteExpenses(it) }
    }

    @Query("DELETE FROM expense WHERE id IN (:ids)")
    suspend fun deleteExpenses(ids: List<String>)
}
