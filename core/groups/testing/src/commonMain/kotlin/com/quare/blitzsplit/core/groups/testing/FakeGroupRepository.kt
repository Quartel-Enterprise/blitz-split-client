package com.quare.blitzsplit.core.groups.testing

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// In-memory GroupRepository: the state is public so a test can set and read it, every call is
// recorded, and the failure knobs are public vars.
class FakeGroupRepository(
    initialGroups: List<Group> = emptyList(),
) : GroupRepository {
    val groups = MutableStateFlow(initialGroups)
    val hasPendingChanges = MutableStateFlow(false)
    val createdGroups = mutableListOf<Group>()
    val createdAts = mutableListOf<Long>()
    val uploadedUserIds = mutableListOf<String>()
    val downloadedUserIds = mutableListOf<String>()
    var createError: Throwable? = null
    val addedMembers = mutableListOf<Pair<String, GroupMember>>()
    val memberChangeTimes = mutableListOf<Long>()
    val renamedMembers = mutableListOf<Pair<String, String>>()
    val removedMemberIds = mutableListOf<String>()
    val updatedGroupIds = mutableListOf<String>()
    val groupUpdateTimes = mutableListOf<Long>()
    var updateGroupError: Throwable? = null

    // Why: while set, an update waits for it, so a test can act while the save is in flight.
    var updateGroupGate: CompletableDeferred<Unit>? = null

    // Why: one knob for the three member edits, which fail alike.
    var memberChangeError: Throwable? = null

    // Why: while set, a create waits for it, so a test can act while the save is in flight.
    var createGate: CompletableDeferred<Unit>? = null
    val rotatedInviteCodeGroupIds = mutableListOf<String>()

    // Why: the code the server makes for the next change of a group's link.
    var nextInviteCode = "newCode1"
    var rotateInviteCodeError: Throwable? = null

    // Why: while set, a change of the link waits for it, so a test can act while the call is in flight.
    var rotateInviteCodeGate: CompletableDeferred<Unit>? = null
    var pushError: Throwable? = null
    var pullError: Throwable? = null
    var clearError: Throwable? = null
    var clearCount = 0

    override fun observeGroups(): Flow<List<Group>> = groups

    override fun observeHasPendingChanges(): Flow<Boolean> = hasPendingChanges

    override suspend fun createGroup(
        group: Group,
        createdAt: Long,
    ): Result<Unit> {
        createGate?.await()
        createError?.let { return Result.failure(it) }
        createdGroups += group
        createdAts += createdAt
        groups.value = listOf(group) + groups.value
        hasPendingChanges.value = true
        return Result.success(Unit)
    }

    override suspend fun updateGroup(
        groupId: String,
        name: String,
        type: GroupType,
        color: AvatarColor,
        currency: GroupCurrency,
        updatedAt: Long,
    ): Result<Unit> {
        updateGroupGate?.await()
        updateGroupError?.let { return Result.failure(it) }
        updatedGroupIds += groupId
        groupUpdateTimes += updatedAt
        groups.value = groups.value.map { group ->
            if (group.id == groupId) {
                group.copy(
                    name = name,
                    type = type,
                    color = color,
                    currency = currency,
                    isPending = true,
                )
            } else {
                group
            }
        }
        hasPendingChanges.value = true
        return Result.success(Unit)
    }

    override suspend fun addMember(
        groupId: String,
        member: GroupMember,
        addedAt: Long,
    ): Result<Unit> = updateMembers(groupId, addedAt) { members ->
        addedMembers += groupId to member
        members + member
    }

    override suspend fun updateMemberName(
        memberId: String,
        name: String,
        updatedAt: Long,
    ): Result<Unit> = updateMembers(groupIdOf(memberId), updatedAt) { members ->
        renamedMembers += memberId to name
        members.map { if (it.id == memberId) it.copy(name = name) else it }
    }

    override suspend fun removeMember(
        memberId: String,
        removedAt: Long,
    ): Result<Unit> = updateMembers(groupIdOf(memberId), removedAt) { members ->
        removedMemberIds += memberId
        members.filterNot { it.id == memberId && !it.isMe && !it.hasJoined }
    }

    private fun groupIdOf(memberId: String): String? =
        groups.value.firstOrNull { group -> group.members.any { it.id == memberId } }?.id

    private fun updateMembers(
        groupId: String?,
        at: Long,
        change: (List<GroupMember>) -> List<GroupMember>,
    ): Result<Unit> {
        memberChangeError?.let { return Result.failure(it) }
        memberChangeTimes += at
        groups.value = groups.value.map { group ->
            if (group.id == groupId) {
                group.copy(
                    members = change(group.members),
                    isPending = true,
                )
            } else {
                group
            }
        }
        hasPendingChanges.value = true
        return Result.success(Unit)
    }

    override suspend fun rotateInviteCode(groupId: String): Result<Unit> {
        rotatedInviteCodeGroupIds += groupId
        rotateInviteCodeGate?.await()
        rotateInviteCodeError?.let { return Result.failure(it) }
        groups.value = groups.value.map { group ->
            if (group.id == groupId) group.copy(inviteCode = nextInviteCode) else group
        }
        return Result.success(Unit)
    }

    override suspend fun uploadPendingGroups(userId: String): Result<Unit> {
        uploadedUserIds += userId
        pushError?.let { return Result.failure(it) }
        hasPendingChanges.value = false
        return Result.success(Unit)
    }

    override suspend fun downloadGroups(userId: String): Result<Unit> {
        downloadedUserIds += userId
        return pullError?.let { Result.failure(it) } ?: Result.success(Unit)
    }

    override suspend fun clear() {
        clearCount++
        clearError?.let { throw it }
        groups.value = emptyList()
        hasPendingChanges.value = false
    }
}
