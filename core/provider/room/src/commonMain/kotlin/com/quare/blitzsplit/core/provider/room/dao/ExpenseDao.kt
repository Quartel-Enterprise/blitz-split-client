package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.quare.blitzsplit.core.provider.room.entity.ExpenseEntity
import com.quare.blitzsplit.core.provider.room.entity.ExpenseWithEditor
import kotlinx.coroutines.flow.Flow

/*
 * Why: MAX_IDS_PER_QUERY stays below SQLite's oldest cap on bound parameters (999), which some devices still have.
 * EDITOR_COLUMN is the member who last saved a bill, through the account the server recorded (ExpenseWithEditor).
 */
private const val MAX_IDS_PER_QUERY = 500
private const val EDITOR_COLUMN = "(SELECT group_member.id FROM group_member WHERE group_member.groupId = " +
    "expense.groupId AND group_member.userId = expense.updatedBy LIMIT 1) AS updatedByMemberId"

@Dao
interface ExpenseDao {
    // Why: the most recent bill first, as the group's list shows them.
    @Query(
        "SELECT expense.*, $EDITOR_COLUMN FROM expense WHERE groupId = :groupId AND isDeleted = 0 " +
            "ORDER BY spentAt DESC, addedAt DESC, id",
    )
    fun observeExpenses(groupId: String): Flow<List<ExpenseWithEditor>>

    // Why: every group's bills in one query, each group's in the order of its own list, for the home's balances.
    @Query(
        "SELECT expense.*, $EDITOR_COLUMN FROM expense WHERE isDeleted = 0 " +
            "ORDER BY groupId, spentAt DESC, addedAt DESC, id",
    )
    fun observeAllExpenses(): Flow<List<ExpenseWithEditor>>

    // Why: "Conta" and "Editar conta", which close once the bill is deleted, here or on another device.
    @Query(
        "SELECT expense.*, $EDITOR_COLUMN FROM expense WHERE id = :id AND isDeleted = 0",
    )
    fun observeExpense(id: String): Flow<ExpenseWithEditor?>

    // Why: the group of each bill deleted here whose tombstone is not on the server yet, once per bill.
    @Query("SELECT groupId FROM expense WHERE pendingSync = 1 AND isDeleted = 1")
    fun observePendingDeletionGroupIds(): Flow<List<String>>

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

    /*
     * Why: "Editar conta" changes what the bill says and keeps who created it, when and in which group. Its version
     * moves past the stored one even on a clock behind the device that saved it last, so Last-Write-Wins takes it.
     * Answers how many bills changed: none once a sync deleted it meanwhile.
     */
    @Query(
        "UPDATE expense SET description = :description, amountMinor = :amountMinor, splitMode = :splitMode, " +
            "payersJson = :payersJson, sharesJson = :sharesJson, spentAt = :spentAt, " +
            "updatedAt = MAX(:now, updatedAt + 1), updatedBy = NULL, pendingSync = 1 " +
            "WHERE id = :id AND isDeleted = 0",
    )
    suspend fun updateExpense(
        id: String,
        description: String,
        amountMinor: Long,
        splitMode: String,
        payersJson: String,
        sharesJson: String,
        spentAt: Long,
        now: Long,
    ): Int

    // Why: "Apagar conta" leaves a tombstone that the sync takes up, so every device drops the bill.
    @Query(
        "UPDATE expense SET isDeleted = 1, updatedAt = MAX(:now, updatedAt + 1), updatedBy = NULL, pendingSync = 1 " +
            "WHERE id = :id AND isDeleted = 0",
    )
    suspend fun markDeleted(
        id: String,
        now: Long,
    ): Int

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
            updatedBy = entity.updatedBy,
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
            "isDeleted = :isDeleted, addedAt = :addedAt, updatedAt = :remoteUpdatedAt, updatedBy = :updatedBy, " +
            "pendingSync = 0 " +
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
        updatedBy: String?,
    )

    // Why: every bill a push took up in one transaction, so the observers of the table wake once rather than per bill.
    @Transaction
    suspend fun markAllSynced(pushed: List<ExpenseEntity>) {
        pushed.forEach {
            markSynced(
                id = it.id,
                syncedUpdatedAt = it.updatedAt,
                updatedBy = it.updatedBy,
            )
        }
    }

    /*
     * Why: a change made while the push was in flight keeps the bill pending. Who pushed is who the server recorded,
     * which the next download can't bring: it only takes a newer version.
     */
    @Query(
        "UPDATE expense SET pendingSync = 0, updatedBy = :updatedBy WHERE id = :id AND updatedAt = :syncedUpdatedAt",
    )
    suspend fun markSynced(
        id: String,
        syncedUpdatedAt: Long,
        updatedBy: String?,
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
