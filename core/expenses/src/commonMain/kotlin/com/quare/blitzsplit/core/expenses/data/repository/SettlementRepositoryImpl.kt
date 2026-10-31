package com.quare.blitzsplit.core.expenses.data.repository

import com.quare.blitzsplit.core.expenses.data.datasource.SettlementLocalDataSource
import com.quare.blitzsplit.core.expenses.data.datasource.SettlementRemoteDataSource
import com.quare.blitzsplit.core.expenses.data.mapper.SettlementMapper
import com.quare.blitzsplit.core.expenses.domain.model.Settlement
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class SettlementRepositoryImpl(
    private val local: SettlementLocalDataSource,
    private val remote: SettlementRemoteDataSource,
    private val mapper: SettlementMapper,
) : SettlementRepository {
    override fun observeSettlements(groupId: String): Flow<List<Settlement>> = local
        .observeSettlements(groupId)
        .map { entities -> entities.map(mapper::toDomain) }
        .distinctUntilChanged()

    override fun observeAllSettlements(): Flow<List<Settlement>> = local
        .observeAllSettlements()
        .map { entities -> entities.map(mapper::toDomain) }
        .distinctUntilChanged()

    override fun observePendingDeletionGroupIds(): Flow<List<String>> = local
        .observePendingDeletionGroupIds()
        .distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    override fun observePendingVersions(): Flow<List<Long>> = local.observePendingVersions().distinctUntilChanged()

    override suspend fun createSettlement(
        settlement: Settlement,
        recordedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.insertSettlement(
            mapper.toEntity(
                settlement = settlement,
                recordedAt = recordedAt,
            ),
        )
    }

    override suspend fun deleteSettlement(
        id: String,
        deletedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.markDeleted(
            id = id,
            now = deletedAt,
        )
    }

    // Why: only the payments of this user's groups, which went up before them in the same sync.
    override suspend fun uploadPendingSettlements(userId: String): Result<Unit> = suspendRunCatching {
        val pending = local.getPendingSettlements(userId)
        if (pending.isEmpty()) return@suspendRunCatching
        remote.upsertSettlements(pending.map { mapper.toDto(it, userId) })
        // Why: one recorded before any account went up as recorded by whoever pushed it.
        local.markSynced(pending.map { it.copy(createdBy = it.createdBy ?: userId) })
    }

    override suspend fun downloadSettlements(userId: String): Result<Unit> = suspendRunCatching {
        val remoteSettlements = remote.fetchSettlements()
        local.applyRemote(remoteSettlements.map(mapper::toEntity))
        local.deleteGoneFromServer(
            ownerUserId = userId,
            serverSettlementIds = remoteSettlements.map { it.id }.toSet(),
        )
    }
}
