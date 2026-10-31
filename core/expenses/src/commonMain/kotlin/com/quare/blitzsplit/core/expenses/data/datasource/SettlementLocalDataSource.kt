package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

interface SettlementLocalDataSource {
    fun observeSettlements(groupId: String): Flow<List<SettlementEntity>>

    fun observeAllSettlements(): Flow<List<SettlementEntity>>

    fun observeHasPending(): Flow<Boolean>

    fun observePendingVersions(): Flow<List<Long>>

    suspend fun getPendingSettlements(ownerUserId: String): List<SettlementEntity>

    // Why: fails when the group is no longer on the device, which a sync may have removed meanwhile.
    suspend fun insertSettlement(entity: SettlementEntity)

    // Why: a payment already gone, which a sync deleted meanwhile, is what was asked, so only a disk error fails.
    suspend fun markDeleted(
        id: String,
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
