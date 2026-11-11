package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import com.quare.blitzsplit.core.provider.room.entity.MemberPhotoEntity
import kotlinx.coroutines.flow.Flow

interface GroupLocalDataSource {
    fun observeGroups(): Flow<List<GroupEntity>>

    fun observeHasGroups(): Flow<Boolean>

    fun observeMembers(): Flow<List<GroupMemberEntity>>

    fun observeHasPending(): Flow<Boolean>

    fun observeMemberPhotos(): Flow<List<MemberPhotoEntity>>

    // Why: the server's photos take the place of every one the device had, so a removed photo leaves too.
    suspend fun replaceMemberPhotos(entities: List<MemberPhotoEntity>)

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

    suspend fun updateGroupPhoto(
        id: String,
        photoUrl: String?,
        pendingPhoto: ByteArray?,
        updatedAt: Long,
    )

    suspend fun updateGroup(
        id: String,
        name: String,
        type: String,
        color: String,
        // Why: null keeps the stored one.
        shape: String?,
        photoUrl: String?,
        pendingPhoto: ByteArray?,
        currency: String,
        updatedAt: Long,
    )

    // Why: "Divisão padrão" and the incomes "Pela renda" was typed with, which only this device keeps.
    suspend fun updateDefaultSplit(
        id: String,
        defaultSplit: String?,
        splitIncomes: String?,
        updatedAt: Long,
    )

    // Why: "Simplificar dívidas" saves only itself, with the pairs it changed for Atividade (13.7).
    suspend fun updateSimplifyDebts(
        id: String,
        simplifyDebts: Boolean,
        change: String?,
        changedAt: Long,
        by: String?,
    )

    suspend fun markSimplifyDebtsSynced(
        id: String,
        syncedAt: Long,
    )

    suspend fun clearSimplifyDebtsConflict(id: String)

    suspend fun getSplitIncomes(id: String): String?

    // Why: the stored JSON, so a new "Divisão padrão" keeps the keys a newer app wrote in it.
    suspend fun getDefaultSplit(id: String): String?

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

    suspend fun markGroupPhotoUploaded(
        id: String,
        jpeg: ByteArray,
        photoUrl: String,
    )

    suspend fun dropRefusedGroupPhoto(
        id: String,
        jpeg: ByteArray,
    )

    suspend fun markGroupSynced(
        id: String,
        syncedUpdatedAt: Long,
        // Why: the switch of "Simplificar dívidas" that went up, whose later change keeps the group pending.
        syncedSimplifyDebtsAt: Long,
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

    // Why: the groups an upload took up, so a later download that misses them reads as removed there.
    suspend fun markGroupsOnServer(ids: List<String>)

    // Why: another group took the code this device made, so the group goes up again with a new one.
    suspend fun replaceInviteCodeBeforeUpload(
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

    // Why: the user's groups the server had at some point, whatever changed here since.
    suspend fun getUploadedGroupIds(ownerUserId: String): List<String>

    // Why: with their members, bills and payments, even what is still to push: the server no longer has them here.
    suspend fun deleteGroups(ids: List<String>)

    suspend fun clear()
}
