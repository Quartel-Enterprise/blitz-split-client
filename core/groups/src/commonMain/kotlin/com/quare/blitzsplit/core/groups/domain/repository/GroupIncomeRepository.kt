package com.quare.blitzsplit.core.groups.domain.repository

import kotlinx.coroutines.flow.Flow

/*
 * Why: each member's income for "By income" (Default split's way and a bill's Split mode), shared with the group's
 * members and synced Last-Write-Wins per member, offline first like the rest of the group.
 */
interface GroupIncomeRepository {
    // Why: by member id in minor units of the group's currency, only the ones above 0.
    fun observeIncomes(groupId: String): Flow<Map<String, Long>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: the versions waiting to go up, which change with each income saved, so each one queues a sync.
    fun observePendingVersions(): Flow<List<Long>>

    /*
     * Why: the incomes of the members given, by id in minor units; 0 removes one, and a member left out keeps theirs.
     * Only the ones that changed wait to go up.
     */
    suspend fun saveIncomes(
        groupId: String,
        incomes: Map<String, Long>,
        updatedAt: Long,
    ): Result<Unit>

    // Why: after the groups, since an income needs its group on the device.
    suspend fun download(userId: String): Result<Unit>

    // Why: after the groups and their members went up, which the server checks an income against.
    suspend fun uploadPending(userId: String): Result<Unit>

    // Why: down then up, as one part of the sync that never holds the others back.
    suspend fun sync(userId: String): Result<Unit>

    // Why: one group's incomes for a first download or real time, read now and written with the group's other rows.
    suspend fun fetchGroupIncomes(groupId: String): Result<suspend () -> Unit>
}
