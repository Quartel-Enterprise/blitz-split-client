package com.quare.blitzsplit.core.expenses.testing

import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// In-memory SettlementRepository: the state is public so a test can set and read it, every call is
// recorded, and the failure knobs are public vars.
class FakeSettlementRepository(
    initialSettlements: List<Settlement> = emptyList(),
) : SettlementRepository {
    val settlements = MutableStateFlow(initialSettlements)
    val hasPendingChanges = MutableStateFlow(false)
    val pendingVersions = MutableStateFlow(emptyList<Long>())
    val recordedSettlements = mutableListOf<Settlement>()
    val recordedAts = mutableListOf<Long>()
    val deletedIds = mutableListOf<String>()
    val deletedAts = mutableListOf<Long>()
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var recordError: Throwable? = null
    var deleteError: Throwable? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null

    // Why: while set, a record waits for it, so a test can act while the save is in flight.
    var recordGate: CompletableDeferred<Unit>? = null

    // Why: like Room, the group's payments with the most recent first.
    override fun observeSettlements(groupId: String): Flow<List<Settlement>> = settlements.map { list ->
        list.filter { it.groupId == groupId }.sortedByDescending { it.paidAt }
    }

    // Why: like Room, each group's payments together, the most recent first.
    override fun observeAllSettlements(): Flow<List<Settlement>> = settlements.map { list ->
        list.sortedWith(compareBy<Settlement> { it.groupId }.thenByDescending { it.paidAt })
    }

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override fun observePendingVersions(): Flow<List<Long>> = pendingVersions

    override suspend fun createSettlement(
        settlement: Settlement,
        recordedAt: Long,
    ): Result<Unit> {
        recordGate?.await()
        recordError?.let { return Result.failure(it) }
        recordedSettlements += settlement
        recordedAts += recordedAt
        settlements.update { it + settlement }
        return Result.success(Unit)
    }

    override suspend fun deleteSettlement(
        id: String,
        deletedAt: Long,
    ): Result<Unit> {
        deleteError?.let { return Result.failure(it) }
        // Why: like Room, a payment already gone is what was asked.
        if (settlements.value.none { it.id == id }) return Result.success(Unit)
        deletedIds += id
        deletedAts += deletedAt
        settlements.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }

    override suspend fun uploadPendingSettlements(userId: String): Result<Unit> {
        uploadedUserIds += userId
        pushError?.let { return Result.failure(it) }
        hasPendingChanges.value = false
        pendingVersions.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun downloadSettlements(userId: String): Result<Unit> {
        downloadedUserIds += userId
        return pullError?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}
