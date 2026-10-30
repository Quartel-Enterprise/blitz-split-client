package com.quare.blitzsplit.core.provider.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

// Why: below SQLite's oldest cap on bound parameters (999), which some devices still have.
private const val MAX_IDS_PER_QUERY = 500

@Dao
interface GroupDao {
    @Query("SELECT * FROM split_group ORDER BY updatedAt DESC, id")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM group_member ORDER BY addedAt, id")
    fun observeMembers(): Flow<List<GroupMemberEntity>>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM split_group WHERE pendingSync = 1) " +
            "OR EXISTS(SELECT 1 FROM group_member WHERE pendingSync = 1)",
    )
    fun observeHasPending(): Flow<Boolean>

    @Query("SELECT * FROM split_group WHERE ownerUserId = :ownerUserId AND pendingSync = 1")
    suspend fun getPendingGroups(ownerUserId: String): List<GroupEntity>

    @Query(
        "SELECT * FROM group_member WHERE pendingSync = 1 AND isRemoved = 0 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getPendingMembers(ownerUserId: String): List<GroupMemberEntity>

    @Query(
        "SELECT id FROM group_member WHERE isRemoved = 1 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getRemovedMemberIds(ownerUserId: String): List<String>

    @Query("SELECT id FROM split_group WHERE ownerUserId = :ownerUserId AND pendingSync = 0")
    suspend fun getSyncedGroupIds(ownerUserId: String): List<String>

    @Query(
        "SELECT * FROM group_member WHERE pendingSync = 0 AND groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId = :ownerUserId)",
    )
    suspend fun getSyncedMembers(ownerUserId: String): List<GroupMemberEntity>

    @Upsert
    suspend fun upsertGroup(entity: GroupEntity)

    @Upsert
    suspend fun upsertMembers(entities: List<GroupMemberEntity>)

    // Why: one write, so nobody sees the group without its members.
    @Transaction
    suspend fun insertGroupWithMembers(
        group: GroupEntity,
        members: List<GroupMemberEntity>,
    ) {
        upsertGroup(group)
        upsertMembers(members)
    }

    // Why: a group only the server has comes down whole; one the device has follows Last-Write-Wins, in one write.
    @Transaction
    suspend fun applyRemoteGroup(entity: GroupEntity) {
        insertGroupIfAbsent(entity)
        updateGroupFromRemote(
            id = entity.id,
            name = entity.name,
            type = entity.type,
            color = entity.color,
            currency = entity.currency,
            createdBy = entity.createdBy,
            addedAt = entity.addedAt,
            remoteUpdatedAt = entity.updatedAt,
            ownerUserId = requireNotNull(entity.ownerUserId),
        )
        entity.inviteCode?.let {
            updateInviteCode(
                id = entity.id,
                inviteCode = it,
            )
        }
    }

    @Transaction
    suspend fun applyRemoteMember(entity: GroupMemberEntity) {
        insertMemberIfAbsent(entity)
        updateMemberFromRemote(
            id = entity.id,
            groupId = entity.groupId,
            name = entity.name,
            avatarColor = entity.avatarColor,
            userId = entity.userId,
            isMe = entity.isMe,
            addedAt = entity.addedAt,
            remoteUpdatedAt = entity.updatedAt,
        )
        entity.inviteCode?.let {
            updateMemberInviteCode(
                id = entity.id,
                inviteCode = it,
            )
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroupIfAbsent(entity: GroupEntity)

    /*
     * Why: a group a sync deleted in the meantime takes nobody. Its member would stay pending with no group to go up
     * with, and keep the sync running round after round.
     */
    @Transaction
    suspend fun insertMemberIntoGroup(entity: GroupMemberEntity) {
        check(hasGroup(entity.groupId)) { "No group ${entity.groupId}" }
        insertMember(entity)
    }

    @Query("SELECT EXISTS(SELECT 1 FROM split_group WHERE id = :id)")
    suspend fun hasGroup(id: String): Boolean

    @Insert
    suspend fun insertMember(entity: GroupMemberEntity)

    @Query(
        "UPDATE group_member SET name = :name, updatedAt = :updatedAt, pendingSync = 1 " +
            "WHERE id = :id AND isRemoved = 0",
    )
    suspend fun renameMember(
        id: String,
        name: String,
        updatedAt: Long,
    )

    /*
     * Why: "Editar grupo" changes the group's own fields; who created it, its members and its invite code stay. The
     * edit is stamped after the row it changes, even when that row came from a device whose clock runs ahead, or
     * Last-Write-Wins would drop it on the next sync.
     */
    @Query(
        "UPDATE split_group SET name = :name, type = :type, color = :color, currency = :currency, " +
            "updatedAt = MAX(:updatedAt, updatedAt + 1), pendingSync = 1 WHERE id = :id",
    )
    suspend fun updateGroup(
        id: String,
        name: String,
        type: String,
        color: String,
        currency: String,
        updatedAt: Long,
    )

    // Why: someone who joined sees the group too, so only a member nobody is yet can be removed.
    @Query(
        "UPDATE group_member SET isRemoved = 1, updatedAt = :removedAt, pendingSync = 1 " +
            "WHERE id = :id AND isMe = 0 AND userId IS NULL",
    )
    suspend fun removeMember(
        id: String,
        removedAt: Long,
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMemberIfAbsent(entity: GroupMemberEntity)

    // Why: a change made while the push was in flight keeps the group pending.
    @Query("UPDATE split_group SET pendingSync = 0 WHERE id = :id AND updatedAt = :syncedUpdatedAt")
    suspend fun markGroupSynced(
        id: String,
        syncedUpdatedAt: Long,
    )

    /*
     * Why: the server makes the code and no client changes it, so it is taken whatever Last-Write-Wins says: a group
     * this device created comes back with the same updatedAt, which updateGroupFromRemote leaves alone. A code already
     * stored is not written again, so a sync doesn't wake every observer of the groups once per group.
     */
    @Query("UPDATE split_group SET inviteCode = :inviteCode WHERE id = :id AND inviteCode IS NOT :inviteCode")
    suspend fun updateInviteCode(
        id: String,
        inviteCode: String,
    )

    // Why: the codes an upload brings back, written at once so whoever observes the members redraws once.
    @Transaction
    suspend fun updateMemberInviteCodes(codesById: Map<String, String>) {
        codesById.forEach { (id, inviteCode) ->
            updateMemberInviteCode(
                id = id,
                inviteCode = inviteCode,
            )
        }
    }

    // Why: like the group's, the server makes the member's code and no client changes it.
    @Query("UPDATE group_member SET inviteCode = :inviteCode WHERE id = :id AND inviteCode IS NOT :inviteCode")
    suspend fun updateMemberInviteCode(
        id: String,
        inviteCode: String,
    )

    @Query("UPDATE group_member SET pendingSync = 0 WHERE id = :id AND updatedAt = :syncedUpdatedAt")
    suspend fun markMemberSynced(
        id: String,
        syncedUpdatedAt: Long,
    )

    /*
     * Why: Last-Write-Wins as the server keeps it. Other members edit the same group, so a newer server row wins
     * even over a change made offline, which the server would refuse as older anyway; for a member, that includes a
     * removal made here, which then comes back.
     */
    @Query(
        "UPDATE split_group SET name = :name, type = :type, color = :color, currency = :currency, " +
            "createdBy = :createdBy, addedAt = :addedAt, updatedAt = :remoteUpdatedAt, pendingSync = 0 " +
            "WHERE id = :id AND ownerUserId = :ownerUserId AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun updateGroupFromRemote(
        id: String,
        name: String,
        type: String,
        color: String,
        currency: String,
        createdBy: String?,
        addedAt: Long,
        remoteUpdatedAt: Long,
        ownerUserId: String,
    )

    @Query(
        "UPDATE group_member SET groupId = :groupId, name = :name, avatarColor = :avatarColor, userId = :userId, " +
            "isMe = :isMe, addedAt = :addedAt, updatedAt = :remoteUpdatedAt, pendingSync = 0, isRemoved = 0 " +
            "WHERE id = :id AND updatedAt < :remoteUpdatedAt",
    )
    suspend fun updateMemberFromRemote(
        id: String,
        groupId: String,
        name: String,
        avatarColor: String,
        userId: String?,
        isMe: Boolean,
        addedAt: Long,
        remoteUpdatedAt: Long,
    )

    // Why: groups created before the account belong to the first user a sync runs under, and so does its member.
    @Transaction
    suspend fun claimUnowned(ownerUserId: String) {
        claimUnownedGroups(ownerUserId)
        claimOwnMembers(ownerUserId)
    }

    @Query(
        "UPDATE split_group SET ownerUserId = :ownerUserId, createdBy = COALESCE(createdBy, :ownerUserId) " +
            "WHERE ownerUserId IS NULL",
    )
    suspend fun claimUnownedGroups(ownerUserId: String)

    @Query("UPDATE group_member SET userId = :userId WHERE isMe = 1 AND userId IS NULL")
    suspend fun claimOwnMembers(userId: String)

    // Why: another user's groups, after a switch, never go up as this user's, even ones not pushed yet.
    @Transaction
    suspend fun deleteOtherUsersGroups(ownerUserId: String) {
        deleteMembersOfOtherUsersGroups(ownerUserId)
        deleteOtherUsersGroupRows(ownerUserId)
    }

    @Query(
        "DELETE FROM group_member WHERE groupId IN " +
            "(SELECT id FROM split_group WHERE ownerUserId != :ownerUserId)",
    )
    suspend fun deleteMembersOfOtherUsersGroups(ownerUserId: String)

    @Query("DELETE FROM split_group WHERE ownerUserId != :ownerUserId")
    suspend fun deleteOtherUsersGroupRows(ownerUserId: String)

    /*
     * Why: the synced groups the server no longer gives this user were removed there, with all their members; so
     * were the synced members missing from a group the server still has. A group edited here keeps its members.
     */
    @Transaction
    suspend fun deleteGoneFromServer(
        ownerUserId: String,
        serverGroupIds: Set<String>,
        serverMemberIds: Set<String>,
    ) {
        val goneGroups = getSyncedGroupIds(ownerUserId).filterNot { it in serverGroupIds }
        val goneMembers = getSyncedMembers(ownerUserId)
            .filter { it.groupId in serverGroupIds && it.id !in serverMemberIds }
            .map { it.id }
        goneGroups.chunked(MAX_IDS_PER_QUERY).forEach {
            deleteMembersOfGroups(it)
            deleteGroups(it)
        }
        goneMembers.chunked(MAX_IDS_PER_QUERY).forEach { deleteMembers(it) }
    }

    @Query("DELETE FROM group_member WHERE groupId IN (:groupIds)")
    suspend fun deleteMembersOfGroups(groupIds: List<String>)

    @Query("DELETE FROM split_group WHERE id IN (:ids)")
    suspend fun deleteGroups(ids: List<String>)

    @Query("DELETE FROM group_member WHERE id IN (:ids)")
    suspend fun deleteMembers(ids: List<String>)

    // Why: a member a newer server row brought back in the meantime is no longer removed, and stays.
    @Transaction
    suspend fun deleteRemovedMembers(ids: List<String>) {
        ids.chunked(MAX_IDS_PER_QUERY).forEach { deleteRemovedMemberRows(it) }
    }

    @Query("DELETE FROM group_member WHERE id IN (:ids) AND isRemoved = 1")
    suspend fun deleteRemovedMemberRows(ids: List<String>)

    @Transaction
    suspend fun deleteAll() {
        deleteAllMembers()
        deleteAllGroups()
    }

    @Query("DELETE FROM group_member")
    suspend fun deleteAllMembers()

    @Query("DELETE FROM split_group")
    suspend fun deleteAllGroups()
}
