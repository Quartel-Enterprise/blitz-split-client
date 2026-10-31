package com.quare.blitzsplit.core.groups.data.datasource

import com.quare.blitzsplit.core.provider.room.dao.GroupDao
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

class RoomGroupLocalDataSource(
    private val dao: GroupDao,
) : GroupLocalDataSource {
    override fun observeGroups(): Flow<List<GroupEntity>> = dao.observeGroups()

    override fun observeMembers(): Flow<List<GroupMemberEntity>> = dao.observeMembers()

    override fun observeHasPending(): Flow<Boolean> = dao.observeHasPending()

    override suspend fun getPendingGroups(ownerUserId: String): List<GroupEntity> = dao.getPendingGroups(ownerUserId)

    override suspend fun getPendingMembers(ownerUserId: String): List<GroupMemberEntity> =
        dao.getPendingMembers(ownerUserId)

    override suspend fun getRemovedMemberIds(ownerUserId: String): List<String> = dao.getRemovedMemberIds(ownerUserId)

    override suspend fun insertMember(entity: GroupMemberEntity) {
        dao.insertMemberIntoGroup(entity)
    }

    override suspend fun renameMember(
        id: String,
        name: String,
        updatedAt: Long,
    ) {
        dao.renameMember(
            id = id,
            name = name,
            updatedAt = updatedAt,
        )
    }

    override suspend fun updateGroup(
        id: String,
        name: String,
        type: String,
        color: String,
        currency: String,
        updatedAt: Long,
    ) {
        dao.updateGroup(
            id = id,
            name = name,
            type = type,
            color = color,
            currency = currency,
            updatedAt = updatedAt,
        )
    }

    override suspend fun removeMember(
        id: String,
        removedAt: Long,
    ) {
        dao.removeMember(
            id = id,
            removedAt = removedAt,
        )
    }

    override suspend fun deleteRemovedMembers(ids: List<String>) {
        dao.deleteRemovedMembers(ids)
    }

    override suspend fun insertGroupWithMembers(
        group: GroupEntity,
        members: List<GroupMemberEntity>,
    ) {
        dao.insertGroupWithMembers(
            group = group,
            members = members,
        )
    }

    override suspend fun markGroupSynced(
        id: String,
        syncedUpdatedAt: Long,
    ) {
        dao.markGroupSynced(
            id = id,
            syncedUpdatedAt = syncedUpdatedAt,
        )
    }

    override suspend fun markMemberSynced(
        id: String,
        syncedUpdatedAt: Long,
    ) {
        dao.markMemberSynced(
            id = id,
            syncedUpdatedAt = syncedUpdatedAt,
        )
    }

    override suspend fun applyRemoteGroup(entity: GroupEntity) {
        dao.applyRemoteGroup(entity)
    }

    override suspend fun updateInviteCode(
        id: String,
        inviteCode: String,
    ) {
        dao.updateInviteCode(
            id = id,
            inviteCode = inviteCode,
        )
    }

    override suspend fun updateMemberInviteCodes(codesById: Map<String, String>) {
        dao.updateMemberInviteCodes(codesById)
    }

    override suspend fun applyRemoteMember(entity: GroupMemberEntity) {
        dao.applyRemoteMember(entity)
    }

    override suspend fun claimUnowned(ownerUserId: String) {
        dao.claimUnowned(ownerUserId)
    }

    override suspend fun deleteOtherUsersGroups(ownerUserId: String) {
        dao.deleteOtherUsersGroups(ownerUserId)
    }

    override suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverGroupIds: Set<String>,
        serverMemberIds: Set<String>,
    ) {
        dao.deleteGoneFromServer(
            ownerUserId = ownerUserId,
            serverGroupIds = serverGroupIds,
            serverMemberIds = serverMemberIds,
        )
    }

    override suspend fun getUploadedGroupIds(ownerUserId: String): List<String> = dao.getUploadedGroupIds(ownerUserId)

    override suspend fun deleteGroups(ids: List<String>) {
        dao.deleteGroupsWithMembers(ids)
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}
