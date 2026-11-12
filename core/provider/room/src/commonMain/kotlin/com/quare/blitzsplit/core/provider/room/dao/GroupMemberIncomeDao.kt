package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberIncomeEntity
import kotlinx.coroutines.flow.Flow

// Why: below SQLite's oldest cap on bound parameters (999), which some devices still have.
private const val MAX_MEMBER_IDS_PER_QUERY = 500

@Dao
interface GroupMemberIncomeDao {
    @Query("SELECT * FROM group_member_income WHERE groupId = :groupId ORDER BY memberId")
    fun observeIncomes(groupId: String): Flow<List<GroupMemberIncomeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM group_member_income WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT updatedAt FROM group_member_income WHERE pendingSync = 1 ORDER BY updatedAt, memberId")
    fun observePendingUpdatedAts(): Flow<List<Long>>

    @Query(
        "SELECT * FROM group_member_income WHERE pendingSync = 1 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getPending(ownerUserId: String): List<GroupMemberIncomeEntity>

    @Query(
        "SELECT memberId FROM group_member_income WHERE pendingSync = 0 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getSyncedMemberIds(ownerUserId: String): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM split_group WHERE id = :id)")
    suspend fun hasGroup(id: String): Boolean

    /*
     * Why: each income typed, stamped past the stored one even on a clock behind the device that saved it last; one
     * unchanged keeps its version, so saving the same incomes again sends nothing. A group a sync deleted meanwhile
     * takes none.
     */
    @Transaction
    suspend fun saveIncomes(
        groupId: String,
        incomes: Map<String, Long>,
        now: Long,
    ) {
        if (!hasGroup(groupId)) return
        incomes.forEach { (memberId, incomeMinor) ->
            insertIfAbsent(
                GroupMemberIncomeEntity(
                    memberId = memberId,
                    groupId = groupId,
                    incomeMinor = 0L,
                    updatedAt = 0L,
                    pendingSync = false,
                ),
            )
            updateIncome(
                memberId = memberId,
                incomeMinor = incomeMinor,
                now = now,
            )
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: GroupMemberIncomeEntity)

    @Query(
        "UPDATE group_member_income SET incomeMinor = :incomeMinor, updatedAt = MAX(:now, updatedAt + 1), " +
            "pendingSync = 1 WHERE memberId = :memberId AND incomeMinor != :incomeMinor",
    )
    suspend fun updateIncome(
        memberId: String,
        incomeMinor: Long,
        now: Long,
    )

    // Why: an income only the server has comes down whole; one the device has follows Last-Write-Wins.
    @Transaction
    suspend fun applyRemote(entities: List<GroupMemberIncomeEntity>) {
        entities.filter { hasGroup(it.groupId) }.forEach { entity ->
            insertIfAbsent(entity)
            updateFromRemote(
                memberId = entity.memberId,
                incomeMinor = entity.incomeMinor,
                remoteUpdatedAt = entity.updatedAt,
            )
        }
    }

    // Why: Last-Write-Wins as the server keeps it, a newer server row winning even over a change not pushed yet.
    @Query(
        "UPDATE group_member_income SET incomeMinor = :incomeMinor, updatedAt = :remoteUpdatedAt, pendingSync = 0 " +
            "WHERE memberId = :memberId AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun updateFromRemote(
        memberId: String,
        incomeMinor: Long,
        remoteUpdatedAt: Long,
    )

    // Why: every income a push took up in one transaction, so the observers wake once.
    @Transaction
    suspend fun markAllSynced(pushed: List<GroupMemberIncomeEntity>) {
        pushed.forEach { markSynced(it.memberId, it.updatedAt) }
    }

    // Why: a change made while the push was in flight keeps the income pending.
    @Query("UPDATE group_member_income SET pendingSync = 0 WHERE memberId = :memberId AND updatedAt = :syncedUpdatedAt")
    suspend fun markSynced(
        memberId: String,
        syncedUpdatedAt: Long,
    )

    // Why: the server answers every income of the user's groups, so a synced one missing there went with its member.
    @Transaction
    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverMemberIds: Set<String>,
    ) {
        getSyncedMemberIds(ownerUserId)
            .filterNot { it in serverMemberIds }
            .chunked(MAX_MEMBER_IDS_PER_QUERY)
            .forEach { deleteIncomes(it) }
    }

    @Query("DELETE FROM group_member_income WHERE memberId IN (:memberIds)")
    suspend fun deleteIncomes(memberIds: List<String>)
}
