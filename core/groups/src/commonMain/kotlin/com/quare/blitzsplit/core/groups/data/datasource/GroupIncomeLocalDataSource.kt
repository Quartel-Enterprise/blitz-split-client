package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.GroupMemberIncomeEntity
import kotlinx.coroutines.flow.Flow

interface GroupIncomeLocalDataSource {
    fun observeIncomes(groupId: String): Flow<List<GroupMemberIncomeEntity>>

    fun observeHasPending(): Flow<Boolean>

    fun observePendingVersions(): Flow<List<Long>>

    suspend fun saveIncomes(
        groupId: String,
        incomes: Map<String, Long>,
        updatedAt: Long,
    )

    suspend fun getPending(ownerUserId: String): List<GroupMemberIncomeEntity>

    suspend fun applyRemote(entities: List<GroupMemberIncomeEntity>)

    suspend fun markSynced(pushed: List<GroupMemberIncomeEntity>)

    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverMemberIds: Set<String>,
    )
}
