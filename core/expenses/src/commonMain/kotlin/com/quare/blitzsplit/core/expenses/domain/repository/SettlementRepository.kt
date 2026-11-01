package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

interface SettlementRepository {
    // Why: the group's payments, the most recent first; an undone one is gone.
    fun observeSettlements(groupId: String): Flow<List<Settlement>>

    // Why: every group's payments at once, each group's in the order of its own list; an undone one is gone.
    fun observeAllSettlements(): Flow<List<Settlement>>

    /*
     * Why: the group of each payment undone here whose tombstone waits to go up, once per payment: gone from the
     * lists, still a change in the queue.
     */
    fun observePendingDeletionGroupIds(): Flow<List<String>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: when each payment waiting to go up was last changed, so a new one triggers a sync after a failed push.
    fun observePendingVersions(): Flow<List<Long>>

    /*
     * Why: on the device first, offline included; the sync takes them to the server after their groups and members.
     * All or none, so a settling up across groups that fails on one leaves nothing waiting to sync.
     */
    suspend fun createSettlements(
        settlements: List<Settlement>,
        recordedAt: Long,
    ): Result<Unit>

    /*
     * Why: "Desfazer" and "Apagar este acerto?", tombstones the sync takes up, all or none; a payment already gone is
     * what was asked.
     */
    suspend fun deleteSettlements(
        ids: List<String>,
        deletedAt: Long,
    ): Result<Unit>

    suspend fun uploadPendingSettlements(userId: String): Result<Unit>

    // Why: after the groups' download, so a payment of a group that just came down has its group on the device.
    suspend fun downloadSettlements(userId: String): Result<Unit>
}
