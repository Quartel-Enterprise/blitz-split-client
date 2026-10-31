package com.quare.blitzsplit.core.groups.data.repository

import com.quare.blitzsplit.core.groups.data.datasource.GroupLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.GroupExitResultDto
import com.quare.blitzsplit.core.groups.data.dto.ReleaseMemberParamsDto
import com.quare.blitzsplit.core.groups.data.mapper.GroupMapper
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.repository.GroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GroupRepositoryImpl(
    private val local: GroupLocalDataSource,
    private val remote: GroupRemoteDataSource,
    private val profileLocal: ProfileLocalDataSource,
    private val mapper: GroupMapper,
    private val deletions: GroupDeletionRepository,
) : GroupRepository {
    override fun observeGroups(): Flow<List<Group>> = combine(
        local.observeGroups(),
        local.observeMembers(),
        mapper::toDomain,
    ).distinctUntilChanged()

    override fun observeHasPendingChanges(): Flow<Boolean> = local.observeHasPending().distinctUntilChanged()

    /*
     * Why: a new group belongs to whoever the profile belongs to, like the payment keys, so a switch drops it with
     * the profile's copy; one made before any account is owned by nobody until a sync claims it.
     */
    override suspend fun createGroup(
        group: Group,
        createdAt: Long,
    ): Result<Unit> = suspendRunCatching {
        val ownerUserId = profileLocal.get()?.ownerUserId
        local.insertGroupWithMembers(
            group = mapper.toEntity(
                group = group,
                createdAt = createdAt,
                ownerUserId = ownerUserId,
            ),
            members = mapper.toMemberEntities(
                group = group,
                createdAt = createdAt,
                ownerUserId = ownerUserId,
            ),
        )
    }

    override suspend fun updateGroup(
        groupId: String,
        name: String,
        type: GroupType,
        color: AvatarColor,
        currency: GroupCurrency,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.updateGroup(
            id = groupId,
            name = name,
            type = type.code,
            color = color.code,
            currency = currency.code,
            updatedAt = updatedAt,
        )
    }

    override suspend fun addMember(
        groupId: String,
        member: GroupMember,
        addedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.insertMember(
            mapper.toMemberEntity(
                groupId = groupId,
                member = member,
                addedAt = addedAt,
            ),
        )
    }

    override suspend fun updateMemberName(
        memberId: String,
        name: String,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.renameMember(
            id = memberId,
            name = name,
            updatedAt = updatedAt,
        )
    }

    override suspend fun removeMember(
        memberId: String,
        removedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.removeMember(
            id = memberId,
            removedAt = removedAt,
        )
    }

    override suspend fun requestLeaveGroup(groupId: String): Result<GroupExitOutcome> = suspendRunCatching {
        exitOutcomeOf(groupId, remote.requestLeaveGroup(groupId))
    }

    override suspend fun deleteGroup(groupId: String): Result<GroupExitOutcome> = suspendRunCatching {
        exitOutcomeOf(groupId, remote.deleteGroup(groupId))
    }

    // Why: not_member means another device already took this person out, so the group leaves this one too.
    private suspend fun exitOutcomeOf(
        groupId: String,
        result: GroupExitResultDto,
    ): GroupExitOutcome = when (result.status) {
        STATUS_HAS_BALANCE -> GroupExitOutcome.HAS_BALANCE

        // Why: the server already agreed, so a local delete that fails is no failure: the next sync drops the group.
        STATUS_LEFT, STATUS_DELETED, STATUS_NOT_MEMBER -> {
            suspendRunCatching { local.deleteGroups(listOf(groupId)) }
            GroupExitOutcome.DONE
        }

        else -> error("Unknown group exit status ${result.status}")
    }

    /*
     * Why: a release only reaches a group the server has, so the device already holds its owner. The server answers a
     * member nobody joined as, which is never this device's user. The account is checked before the call, so a release
     * the server made never reads as failed.
     */
    override suspend fun requestMemberRelease(memberId: String): Result<Unit> = suspendRunCatching {
        val userId = checkNotNull(profileLocal.get()?.ownerUserId) { "No account to release a member with" }
        val released = remote.requestMemberRelease(ReleaseMemberParamsDto(memberId)) ?: return@suspendRunCatching
        local.applyRemoteMember(mapper.toEntity(released, userId = userId))
    }

    /*
     * Why: the groups before their members, which the server only accepts in a group it already has. The members
     * are read first, so a group created between the two reads goes up without them (they wait for the next round)
     * instead of its members going up without it. A new group's invite code comes back with it, and a new member's
     * with it: the sync downloads before it uploads, so waiting for the next download would keep "Convidar" and the
     * personal invite without a link until then. The members removed here go last, and leave the device once the
     * server deleted them.
     */
    override suspend fun uploadPendingGroups(userId: String): Result<Unit> = suspendRunCatching {
        val pendingMembers = local.getPendingMembers(userId)
        val pendingGroups = local.getPendingGroups(userId)
        val removedMemberIds = local.getRemovedMemberIds(userId)
        if (pendingGroups.isNotEmpty()) {
            remote
                .upsertGroups(pendingGroups.map { mapper.toDto(it, userId) })
                .forEach { dto ->
                    dto.inviteCode?.let {
                        local.updateInviteCode(
                            id = dto.id,
                            inviteCode = it,
                        )
                    }
                }
        }
        if (pendingMembers.isNotEmpty()) {
            val codesById = remote
                .upsertMembers(pendingMembers.map(mapper::toDto))
                .mapNotNull { dto -> dto.inviteCode?.let { dto.id to it } }
                .toMap()
            if (codesById.isNotEmpty()) local.updateMemberInviteCodes(codesById)
        }
        // Why: before the deletes, so one that fails doesn't push again what the server already took.
        pendingGroups.forEach {
            local.markGroupSynced(
                id = it.id,
                syncedUpdatedAt = it.updatedAt,
            )
        }
        pendingMembers.forEach {
            local.markMemberSynced(
                id = it.id,
                syncedUpdatedAt = it.updatedAt,
            )
        }
        if (removedMemberIds.isNotEmpty()) {
            remote.deleteMembers(removedMemberIds)
            local.deleteRemovedMembers(removedMemberIds)
        }
    }

    /*
     * Why: like the payment keys, another user's groups never go up as this account's, even ones not pushed yet:
     * the account they belong to has them on the server. Groups made before any account are this user's.
     */
    override suspend fun downloadGroups(userId: String): Result<Unit> = suspendRunCatching {
        val (remoteGroups, remoteMembers) = coroutineScope {
            val groups = async { remote.fetchGroups() }
            val members = async { remote.fetchMembers() }
            groups.await() to members.await()
        }
        local.deleteOtherUsersGroups(userId)
        local.claimUnowned(userId)
        remoteGroups.forEach { dto -> local.applyRemoteGroup(mapper.toEntity(dto, ownerUserId = userId)) }
        // Why: the two reads are separate, so a member whose group the first one didn't see waits for the next sync.
        val remoteGroupIds = remoteGroups.map { it.id }.toSet()
        remoteMembers
            .filter { it.groupId in remoteGroupIds }
            .forEach { dto -> local.applyRemoteMember(mapper.toEntity(dto, userId = userId)) }
        postDeletions(
            userId = userId,
            goneGroupIds = local.getUploadedGroupIds(userId).filterNot { it in remoteGroupIds },
        )
        local.deleteGoneFromServer(
            ownerUserId = userId,
            serverGroupIds = remoteGroupIds,
            serverMemberIds = remoteMembers.map { it.id }.toSet(),
        )
    }

    /*
     * Why: the gone groups leave with deleteGoneFromServer right after; one someone else deleted is posted first, so
     * the screen showing it says "Grupo apagado" with who did it instead of just closing. Only a nicety, so a failed
     * read posts nothing and lets the sync go on.
     */
    private suspend fun postDeletions(
        userId: String,
        goneGroupIds: List<String>,
    ) {
        if (goneGroupIds.isEmpty()) return
        val removed = suspendRunCatching { remote.fetchRemovedGroups(goneGroupIds) }.getOrElse { return }
        removed
            .filter { it.reason == REASON_DELETED && it.removedBy != userId }
            .forEach { dto ->
                deletions.post(
                    GroupDeletion(
                        groupId = dto.groupId,
                        groupName = dto.groupName,
                        deletedByName = dto.removedByName,
                    ),
                )
            }
    }

    override suspend fun clear() {
        local.clear()
    }

    private companion object {
        const val STATUS_LEFT = "left"
        const val STATUS_DELETED = "deleted"
        const val STATUS_HAS_BALANCE = "has_balance"
        const val STATUS_NOT_MEMBER = "not_member"
        const val REASON_DELETED = "deleted"
    }
}
