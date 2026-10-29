package com.quare.blitzsplit.core.groups.domain.repository

import com.quare.blitzsplit.core.groups.domain.model.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    // Why: the most recently changed first.
    fun observeGroups(): Flow<List<Group>>

    fun observeHasPendingChanges(): Flow<Boolean>

    // Why: the group and its members in one write, so nobody sees the group without them.
    suspend fun createGroup(
        group: Group,
        createdAt: Long,
    ): Result<Unit>

    suspend fun uploadPendingGroups(userId: String): Result<Unit>

    /*
     * Why: after a switch, another user's groups give way to this user's, even ones not pushed yet, and the synced
     * groups and members the server no longer has leave the device.
     */
    suspend fun downloadGroups(userId: String): Result<Unit>

    // Why: a sign out leaves no group on the device.
    suspend fun clear()
}
