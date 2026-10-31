package com.quare.blitzsplit.core.expenses.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.SettlementDao
import com.quare.blitzsplit.core.provider.room.entity.SettlementEntity
import kotlinx.coroutines.flow.Flow

class RoomSettlementLocalDataSource(
    private val dao: SettlementDao,
) : SettlementLocalDataSource {
    override fun observeSettlements(groupId: String): Flow<List<SettlementEntity>> = dao.observeSettlements(groupId)

    override fun observeAllSettlements(): Flow<List<SettlementEntity>> = dao.observeAllSettlements()

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override fun observePendingVersions(): Flow<List<Long>> = dao.observePendingUpdatedAts()

    override suspend fun getPendingSettlements(ownerUserId: String): List<SettlementEntity> =
        dao.getPendingSettlements(ownerUserId)

    override suspend fun insertSettlement(entity: SettlementEntity) {
        dao.insertSettlementIntoGroup(entity)
    }

    override suspend fun markDeleted(
        id: String,
        now: Long,
    ) {
        dao.markDeleted(
            id = id,
            now = now,
        )
    }

    override suspend fun applyRemote(entities: List<SettlementEntity>) {
        dao.applyRemoteToGroups(entities)
    }

    override suspend fun markSynced(pushed: List<SettlementEntity>) {
        dao.markAllSynced(pushed)
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverSettlementIds: Set<String>,
    ) {
        dao.deleteGoneFromServer(
            ownerUserId = ownerUserId,
            serverSettlementIds = serverSettlementIds,
        )
    }
}
