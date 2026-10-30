package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

interface GroupLocalDataSource {
    fun observeGroups(): Flow<List<GroupEntity>>

    fun observeMembers(): Flow<List<GroupMemberEntity>>

    fun observeHasPending(): Flow<Boolean>

    suspend fun getPendingGroups(ownerUserId: String): List<GroupEntity>

    // Why: the pending members of the user's groups.
    suspend fun getPendingMembers(ownerUserId: String): List<GroupMemberEntity>

    // Why: the ids of the members removed here, still to delete on the server.
    suspend fun getRemovedMemberIds(ownerUserId: String): List<String>

    // Why: fails when the group is no longer on the device.
    suspend fun insertMember(entity: GroupMemberEntity)

    suspend fun renameMember(
        id: String,
        name: String,
        updatedAt: Long,
    )

    // Why: a member who joined, or this device's user, is left as it is.
    suspend fun removeMember(
        id: String,
        removedAt: Long,
    )

    // Why: once the server deleted them; one a newer server row brought back stays.
    suspend fun deleteRemovedMembers(ids: List<String>)

    suspend fun insertGroupWithMembers(
        group: GroupEntity,
        members: List<GroupMemberEntity>,
    )

    suspend fun markGroupSynced(
        id: String,
        syncedUpdatedAt: Long,
    )

    suspend fun markMemberSynced(
        id: String,
        syncedUpdatedAt: Long,
    )

    suspend fun applyRemoteGroup(entity: GroupEntity)

    suspend fun updateInviteCode(
        id: String,
        inviteCode: String,
    )

    suspend fun updateMemberInviteCodes(codesById: Map<String, String>)

    suspend fun applyRemoteMember(entity: GroupMemberEntity)

    suspend fun claimUnowned(ownerUserId: String)

    suspend fun deleteOtherUsersGroups(ownerUserId: String)

    // Why: the user's synced groups and members missing from the server were removed there; pending ones stay.
    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverGroupIds: Set<String>,
        serverMemberIds: Set<String>,
    )

    suspend fun clear()
}
