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

    // Why: like Room's tombstones, the group of each payment undone here until an upload takes them up.
    val pendingDeletionGroupIds = MutableStateFlow(emptyList<String>())
    val recordedSettlements = mutableListOf<Settlement>()
    val recordedAts = mutableListOf<Long>()
    val deletedIds = mutableListOf<String>()
    val deletedAts = mutableListOf<Long>()

    // Why: the ids of each call, so a test can tell payments undone together from one by one.
    val deleteCalls = mutableListOf<List<String>>()
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

    override fun observePendingDeletionGroupIds(): Flow<List<String>> = pendingDeletionGroupIds

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override fun observePendingVersions(): Flow<List<Long>> = pendingVersions

    // Why: like Room, all or none.
    override suspend fun createSettlements(
        settlements: List<Settlement>,
        recordedAt: Long,
    ): Result<Unit> {
        recordGate?.await()
        recordError?.let { return Result.failure(it) }
        recordedSettlements += settlements
        settlements.forEach { recordedAts += recordedAt }
        this.settlements.update { it + settlements }
        return Result.success(Unit)
    }

    // Why: like Room, all or none, and a payment already gone is what was asked.
    override suspend fun deleteSettlements(
        ids: List<String>,
        deletedAt: Long,
    ): Result<Unit> {
        deleteCalls += ids
        deleteError?.let { return Result.failure(it) }
        val undone = settlements.value.filter { it.id in ids }
        pendingDeletionGroupIds.update { it + undone.map(Settlement::groupId) }
        undone.forEach {
            deletedIds += it.id
            deletedAts += deletedAt
        }
        settlements.update { list -> list.filterNot { it.id in ids } }
        return Result.success(Unit)
    }

    override suspend fun uploadPendingSettlements(userId: String): Result<Unit> {
        uploadedUserIds += userId
        pushError?.let { return Result.failure(it) }
        hasPendingChanges.value = false
        pendingVersions.value = emptyList()
        pendingDeletionGroupIds.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun downloadSettlements(userId: String): Result<Unit> {
        downloadedUserIds += userId
        return pullError?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}
