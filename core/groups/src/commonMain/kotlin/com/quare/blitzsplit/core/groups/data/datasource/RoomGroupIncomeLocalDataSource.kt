package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.GroupMemberIncomeDao
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberIncomeEntity
import kotlinx.coroutines.flow.Flow

class RoomGroupIncomeLocalDataSource(
    private val dao: GroupMemberIncomeDao,
) : GroupIncomeLocalDataSource {
    override fun observeIncomes(groupId: String): Flow<List<GroupMemberIncomeEntity>> = dao.observeIncomes(groupId)

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override fun observePendingVersions(): Flow<List<Long>> = dao.observePendingUpdatedAts()

    override suspend fun saveIncomes(
        groupId: String,
        incomes: Map<String, Long>,
        updatedAt: Long,
    ) {
        dao.saveIncomes(
            groupId = groupId,
            incomes = incomes,
            now = updatedAt,
        )
    }

    override suspend fun getPending(ownerUserId: String): List<GroupMemberIncomeEntity> = dao.getPending(ownerUserId)

    override suspend fun applyRemote(entities: List<GroupMemberIncomeEntity>) {
        dao.applyRemote(entities)
    }

    override suspend fun markSynced(pushed: List<GroupMemberIncomeEntity>) {
        dao.markAllSynced(pushed)
    }

    override suspend fun deleteOfGoneMembers() {
        dao.deleteOfGoneMembers()
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverMemberIds: Set<String>,
    ) {
        dao.deleteGoneFromServer(
            ownerUserId = ownerUserId,
            serverMemberIds = serverMemberIds,
        )
    }
}
