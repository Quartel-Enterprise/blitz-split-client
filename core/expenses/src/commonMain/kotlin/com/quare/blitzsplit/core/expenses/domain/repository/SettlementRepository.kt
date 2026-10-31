package com.quare.blitzsplit.core.expenses.domain.repository

import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

interface SettlementRepository {
    // Why: the group's payments, the most recent first; an undone one is gone.
    fun observeSettlements(groupId: String): Flow<List<Settlement>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: when each payment waiting to go up was last changed, so a new one triggers a sync after a failed push.
    fun observePendingVersions(): Flow<List<Long>>

    // Why: on the device first, offline included; the sync takes it to the server after its group and members.
    suspend fun createSettlement(
        settlement: Settlement,
        recordedAt: Long,
    ): Result<Unit>

    // Why: "Desfazer", a tombstone the sync takes up; fails when the payment is already gone.
    suspend fun deleteSettlement(
        id: String,
        deletedAt: Long,
    ): Result<Unit>

    suspend fun uploadPendingSettlements(userId: String): Result<Unit>

    // Why: after the groups' download, so a payment of a group that just came down has its group on the device.
    suspend fun downloadSettlements(userId: String): Result<Unit>
}
