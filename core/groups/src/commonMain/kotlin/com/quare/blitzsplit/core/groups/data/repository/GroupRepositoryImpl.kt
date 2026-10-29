package com.quare.blitzsplit.core.groups.data.repository

import com.quare.blitzsplit.core.groups.data.datasource.GroupLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.mapper.GroupMapper
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
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

    /*
     * Why: the groups before their members, which the server only accepts in a group it already has. The members
     * are read first, so a group created between the two reads goes up without them (they wait for the next round)
     * instead of its members going up without it.
     */
    override suspend fun uploadPendingGroups(userId: String): Result<Unit> = suspendRunCatching {
        val pendingMembers = local.getPendingMembers(userId)
        val pendingGroups = local.getPendingGroups(userId)
        if (pendingGroups.isNotEmpty()) {
            remote.upsertGroups(pendingGroups.map { mapper.toDto(it, userId) })
        }
        if (pendingMembers.isNotEmpty()) {
            remote.upsertMembers(pendingMembers.map(mapper::toDto))
        }
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
        remoteMembers.forEach { dto -> local.applyRemoteMember(mapper.toEntity(dto, userId = userId)) }
        local.deleteGoneFromServer(
            ownerUserId = userId,
            serverGroupIds = remoteGroups.map { it.id }.toSet(),
            serverMemberIds = remoteMembers.map { it.id }.toSet(),
        )
    }

    override suspend fun clear() {
        local.clear()
    }
}
