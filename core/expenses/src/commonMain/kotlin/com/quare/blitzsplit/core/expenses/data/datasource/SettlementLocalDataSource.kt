package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

interface SettlementLocalDataSource {
    fun observeSettlements(groupId: String): Flow<List<SettlementEntity>>

    fun observeAllSettlements(): Flow<List<SettlementEntity>>

    fun observePendingDeletionGroupIds(): Flow<List<String>>

    fun observeHasPending(): Flow<Boolean>

    fun observePendingVersions(): Flow<List<Long>>

    suspend fun getPendingSettlements(ownerUserId: String): List<SettlementEntity>

    /*
     * Why: all or none, in one transaction; fails when a group is no longer on the device, which a sync may have
     * removed meanwhile.
     */
    suspend fun insertSettlements(entities: List<SettlementEntity>)

    /*
     * Why: all or none, in one transaction. A payment already gone, which a sync deleted meanwhile, is what was asked,
     * so only a disk error fails.
     */
    suspend fun markDeleted(
        ids: List<String>,
        now: Long,
    )

    // Why: skips a payment of a group this device doesn't have, which waits for the sync that brings the group.
    suspend fun applyRemote(entities: List<SettlementEntity>)

    // Why: each payment at the version that went up; one changed meanwhile stays pending.
    suspend fun markSynced(pushed: List<SettlementEntity>)

    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverSettlementIds: Set<String>,
    )
}
