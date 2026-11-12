package com.quare.blitzsplit.core.groups.testing

import com.quare.blitzsplit.core.groups.domain.repository.GroupIncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/*
 * In-memory GroupIncomeRepository: the incomes by group are public so a test can set and read them, and every save and
 * sync call is recorded. Saving merges like Room does: 0 removes one, a member left out keeps theirs.
 */
class FakeGroupIncomeRepository(
    initialIncomes: Map<String, Map<String, Long>> = emptyMap(),
) : GroupIncomeRepository {
    val incomes = MutableStateFlow(initialIncomes)
    val hasPendingChanges = MutableStateFlow(false)
    val pendingVersions = MutableStateFlow<List<Long>>(emptyList())
    val savedIncomes = mutableListOf<Triple<String, Map<String, Long>, Long>>()
    val downloadedUserIds = mutableListOf<String>()
    val uploadedUserIds = mutableListOf<String>()
    val fetchedGroupIds = mutableListOf<String>()
    var saveError: Throwable? = null
    var downloadError: Throwable? = null
    var uploadError: Throwable? = null

    override fun observeIncomes(groupId: String): Flow<Map<String, Long>> = incomes.map { byGroup ->
        byGroup[groupId].orEmpty().filterValues { it > 0 }
    }

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override fun observePendingVersions(): Flow<List<Long>> = pendingVersions

    override suspend fun saveIncomes(
        groupId: String,
        incomes: Map<String, Long>,
        updatedAt: Long,
    ): Result<Unit> {
        savedIncomes += Triple(groupId, incomes, updatedAt)
        saveError?.let { return Result.failure(it) }
        this.incomes.update { byGroup -> byGroup + (groupId to byGroup[groupId].orEmpty() + incomes) }
        hasPendingChanges.value = true
        pendingVersions.update { it + updatedAt }
        return Result.success(Unit)
    }

    override suspend fun download(userId: String): Result<Unit> {
        downloadError?.let { return Result.failure(it) }
        downloadedUserIds += userId
        return Result.success(Unit)
    }

    override suspend fun uploadPending(userId: String): Result<Unit> {
        uploadError?.let { return Result.failure(it) }
        uploadedUserIds += userId
        hasPendingChanges.value = false
        pendingVersions.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun fetchGroupIncomes(groupId: String): Result<suspend () -> Unit> {
        fetchedGroupIds += groupId
        return Result.success {}
    }
}
