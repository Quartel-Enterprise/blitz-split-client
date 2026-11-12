package com.quare.blitzsplit.core.groups.data.repository

import com.quare.blitzsplit.core.groups.data.datasource.GroupIncomeLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupIncomeRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberIncomeDto
import com.quare.blitzsplit.core.groups.domain.repository.GroupIncomeRepository
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberIncomeEntity
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GroupIncomeRepositoryImpl(
    private val local: GroupIncomeLocalDataSource,
    private val remote: GroupIncomeRemoteDataSource,
) : GroupIncomeRepository {
    override fun observeIncomes(groupId: String): Flow<Map<String, Long>> = local
        .observeIncomes(groupId)
        .map { rows -> rows.filter { it.incomeMinor > 0 }.associate { it.memberId to it.incomeMinor } }
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    override fun observePendingVersions(): Flow<List<Long>> = local.observePendingVersions().distinctUntilChanged()

    override suspend fun saveIncomes(
        groupId: String,
        incomes: Map<String, Long>,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.saveIncomes(
            groupId = groupId,
            incomes = incomes.mapValues { (_, income) -> income.coerceAtLeast(0L) },
            updatedAt = updatedAt,
        )
    }

    override suspend fun download(userId: String): Result<Unit> = suspendRunCatching {
        val remoteIncomes = remote.fetchIncomes()
        local.applyRemote(remoteIncomes.map { it.toEntity() })
        local.deleteGoneFromServer(
            ownerUserId = userId,
            serverMemberIds = remoteIncomes.map { it.memberId }.toSet(),
        )
    }

    // Why: only the incomes of this user's groups, which went up before them in the same sync.
    override suspend fun uploadPending(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.getPending(userId)
        if (pending.isEmpty()) return@suspendRunCatching
        remote.upsertIncomes(
            pending.map { income ->
                GroupMemberIncomeDto(
                    memberId = income.memberId,
                    groupId = income.groupId,
                    incomeMinor = income.incomeMinor,
                    updatedBy = userId,
                    updatedAt = income.updatedAt,
                )
            },
        )
        local.markSynced(pending)
    }

    override suspend fun fetchGroupIncomes(groupId: String): Result<suspend () -> Unit> = suspendRunCatching {
        val entities = remote.fetchIncomes(groupId).map { it.toEntity() }
        return@suspendRunCatching { local.applyRemote(entities) }
    }

    private fun GroupMemberIncomeDto.toEntity(): GroupMemberIncomeEntity = GroupMemberIncomeEntity(
        memberId = memberId,
        groupId = groupId,
        incomeMinor = incomeMinor,
        updatedAt = updatedAt,
        pendingSync = false,
    )
}
