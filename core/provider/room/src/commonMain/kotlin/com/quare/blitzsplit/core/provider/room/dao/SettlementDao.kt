package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

// Why: below SQLite's oldest cap on bound parameters (999), which some devices still have.
private const val MAX_IDS_PER_QUERY = 500

@Dao
interface SettlementDao {
    // Why: the most recent payment first, as the group's list shows them among the bills.
    @Query("SELECT * FROM settlement WHERE groupId = :groupId AND isDeleted = 0 ORDER BY paidAt DESC, addedAt DESC, id")
    fun observeSettlements(groupId: String): Flow<List<SettlementEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM settlement WHERE pendingSync = 1)")
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT updatedAt FROM settlement WHERE pendingSync = 1 ORDER BY updatedAt, id")
    fun observePendingUpdatedAts(): Flow<List<Long>>

    @Query(
        "SELECT * FROM settlement WHERE pendingSync = 1 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getPendingSettlements(ownerUserId: String): List<SettlementEntity>

    @Query(
        "SELECT id FROM settlement WHERE pendingSync = 0 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getSyncedSettlementIds(ownerUserId: String): List<String>

    // Why: a group a sync deleted in the meantime takes no payment, which would stay pending with nowhere to go up.
    @Transaction
    suspend fun insertSettlementIntoGroup(entity: SettlementEntity) {
        check(hasGroup(entity.groupId)) { "No group ${entity.groupId}" }
        insertSettlement(entity)
    }

    @Query("SELECT EXISTS(SELECT 1 FROM split_group WHERE id = :id)")
    suspend fun hasGroup(id: String): Boolean

    @Insert
    suspend fun insertSettlement(entity: SettlementEntity)

    /*
     * Why: "Desfazer" leaves a tombstone that the sync takes up, so every device drops the payment. Its version moves
     * past the stored one even on a clock behind the device that recorded it. Answers how many changed: none once a
     * sync deleted it meanwhile.
     */
    @Query(
        "UPDATE settlement SET isDeleted = 1, updatedAt = MAX(:now, updatedAt + 1), pendingSync = 1 " +
            "WHERE id = :id AND isDeleted = 0",
    )
    suspend fun markDeleted(
        id: String,
        now: Long,
    ): Int

    // Why: a payment only the server has comes down whole; one the device has follows Last-Write-Wins, in one write.
    @Transaction
    suspend fun applyRemote(entity: SettlementEntity) {
        insertSettlementIfAbsent(entity)
        updateFromRemote(
            id = entity.id,
            createdBy = entity.createdBy,
            fromMemberId = entity.fromMemberId,
            toMemberId = entity.toMemberId,
            amountMinor = entity.amountMinor,
            method = entity.method,
            paidAt = entity.paidAt,
            isDeleted = entity.isDeleted,
            addedAt = entity.addedAt,
            remoteUpdatedAt = entity.updatedAt,
        )
    }

    // Why: a sync's whole answer in one transaction; a payment of a group this device doesn't have waits for it.
    @Transaction
    suspend fun applyRemoteToGroups(entities: List<SettlementEntity>) {
        entities.filter { hasGroup(it.groupId) }.forEach { applyRemote(it) }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSettlementIfAbsent(entity: SettlementEntity)

    // Why: Last-Write-Wins as the server keeps it, a newer server row winning even over a change not pushed yet.
    @Query(
        "UPDATE settlement SET createdBy = :createdBy, fromMemberId = :fromMemberId, toMemberId = :toMemberId, " +
            "amountMinor = :amountMinor, method = :method, paidAt = :paidAt, isDeleted = :isDeleted, " +
            "addedAt = :addedAt, updatedAt = :remoteUpdatedAt, pendingSync = 0 " +
            "WHERE id = :id AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun updateFromRemote(
        id: String,
        createdBy: String?,
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        method: String,
        paidAt: Long,
        isDeleted: Boolean,
        addedAt: Long,
        remoteUpdatedAt: Long,
    )

    // Why: every payment a push took up in one transaction, so the observers wake once.
    @Transaction
    suspend fun markAllSynced(pushed: List<SettlementEntity>) {
        pushed.forEach {
            markSynced(
                id = it.id,
                syncedUpdatedAt = it.updatedAt,
                createdBy = it.createdBy,
            )
        }
    }

    /*
     * Why: a change made while the push was in flight keeps the payment pending. One recorded before any account
     * went up as recorded by whoever pushed, which the next download can't bring: it only takes a newer version.
     */
    @Query(
        "UPDATE settlement SET pendingSync = 0, createdBy = :createdBy " +
            "WHERE id = :id AND updatedAt = :syncedUpdatedAt",
    )
    suspend fun markSynced(
        id: String,
        syncedUpdatedAt: Long,
        createdBy: String?,
    )

    // Why: the server answers every payment of the user's groups, so a synced one missing there was removed there.
    @Transaction
    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverSettlementIds: Set<String>,
    ) {
        getSyncedSettlementIds(ownerUserId)
            .filterNot { it in serverSettlementIds }
            .chunked(MAX_IDS_PER_QUERY)
            .forEach { deleteSettlements(it) }
    }

    @Query("DELETE FROM settlement WHERE id IN (:ids)")
    suspend fun deleteSettlements(ids: List<String>)
}
