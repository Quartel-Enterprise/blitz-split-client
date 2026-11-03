package com.quare.blitzsplit.core.groups.data.repository

import com.quare.blitzsplit.core.groups.data.datasource.GroupLocalDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupPhotoRemoteDataSource
import com.quare.blitzsplit.core.groups.data.datasource.GroupRemoteDataSource
import com.quare.blitzsplit.core.groups.data.dto.GroupExitResultDto
import com.quare.blitzsplit.core.groups.data.dto.ReleaseMemberParamsDto
import com.quare.blitzsplit.core.groups.data.mapper.GroupMapper
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupDeletion
import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.repository.GroupDeletionRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupShape
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.model.ProfilePhoto
import com.quare.blitzsplit.core.model.toImageUrl
import com.quare.blitzsplit.core.profile.data.datasource.ProfileLocalDataSource
import com.quare.blitzsplit.core.profile.data.mapper.ProfileMapper
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class GroupRepositoryImpl(
    private val local: GroupLocalDataSource,
    private val remote: GroupRemoteDataSource,
    private val photoRemote: GroupPhotoRemoteDataSource,
    private val profileLocal: ProfileLocalDataSource,
    private val profileMapper: ProfileMapper,
    private val mapper: GroupMapper,
    private val deletions: GroupDeletionRepository,
) : GroupRepository {
    override fun observeGroups(): Flow<List<Group>> = combine(
        local.observeGroups(),
        local.observeMembers(),
        local.observeMemberPhotos(),
        observeMyPhotoUrl(),
        mapper::toDomain,
    ).distinctUntilChanged()

    /*
     * Why: the photo as the profile shows it, so one picked offline and not uploaded yet shows in the faces too; the
     * same photo once, so a profile write that keeps it doesn't encode a pending JPEG again.
     */
    private fun observeMyPhotoUrl(): Flow<String?> = profileLocal
        .observe()
        .map { entity -> entity?.let(profileMapper::toDomain)?.photo }
        .distinctUntilChanged()
        .map { it?.toImageUrl() }

    override fun observeHasGroups(): Flow<Boolean> = local.observeHasGroups().distinctUntilChanged()

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
        shape: GroupShape?,
        photo: ProfilePhoto?,
        currency: GroupCurrency,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.updateGroup(
            id = groupId,
            name = name,
            type = type.code,
            color = color.code,
            shape = shape?.code,
            photoUrl = (photo as? ProfilePhoto.Remote)?.url,
            pendingPhoto = (photo as? ProfilePhoto.Pending)?.jpeg,
            currency = currency.code,
            updatedAt = updatedAt,
        )
    }

    /*
     * Why: written over the stored JSON, so the keys a newer app added survive. Back to splitting equally is null,
     * those keys included: the server only takes a default split with a mode.
     */
    override suspend fun updateDefaultSplit(
        groupId: String,
        defaultSplit: GroupDefaultSplit?,
        incomes: Map<String, Long>,
        updatedAt: Long,
    ): Result<Unit> = suspendRunCatching {
        local.updateDefaultSplit(
            id = groupId,
            defaultSplit = defaultSplit?.let {
                mapper.encode(
                    defaultSplit = it,
                    over = local.getDefaultSplit(groupId),
                )
            },
            splitIncomes = mapper.encodeIncomes(incomes),
            updatedAt = updatedAt,
        )
    }

    override suspend fun getSplitIncomes(groupId: String): Result<Map<String, Long>> = suspendRunCatching {
        mapper.decodeIncomes(local.getSplitIncomes(groupId))
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

    override suspend fun rotateInviteCode(groupId: String): Result<Unit> = suspendRunCatching {
        local.updateInviteCode(
            id = groupId,
            inviteCode = remote.rotateInviteCode(groupId),
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
     *
     * A photo picked here goes up before the row that names it, so no device fetches its URL while the object still
     * holds the photo it replaces and caches that one under it. Only a group's members may upload its photo, so a new
     * group's goes up once the group and its members did, its row naming the URL ahead: there is no older photo to
     * show meanwhile. A photo that fails holds its group, and only it, for the next round, so the bills still go up,
     * and its error comes back once the rest did. A photo Storage refuses for good is dropped instead, so the group's
     * other fields still reach the server.
     */
    override suspend fun uploadPendingGroups(userId: String): Result<Unit> = suspendRunCatching {
        val pendingMembers = local.getPendingMembers(userId)
        val removedMemberIds = local.getRemovedMemberIds(userId)
        val heldGroupIds = mutableSetOf<String>()
        val photoErrors = mutableListOf<Throwable>()
        val refusedPhotoGroupIds = mutableSetOf<String>()
        val pendingGroups = local.getPendingGroups(userId)
        pendingGroups.filter { it.isOnServer() }.forEach { group ->
            uploadPhotoOf(group)
                ?.onSuccess { if (it == PhotoUpload.REFUSED) refusedPhotoGroupIds += group.id }
                ?.onFailure {
                    heldGroupIds += group.id
                    photoErrors += it
                }
        }
        val groupsToPush = pendingGroups.filterNot { it.id in heldGroupIds }
        if (groupsToPush.isNotEmpty()) {
            remote
                .upsertGroups(
                    groupsToPush.map {
                        mapper.toDto(
                            entity = it,
                            userId = userId,
                            // Why: the snapshot read above, so an uploaded photo's URL is the one it went up under.
                            photoUrl = if (it.id in refusedPhotoGroupIds) it.photoUrl else photoUrlOf(it),
                        )
                    },
                ).forEach { dto ->
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
        groupsToPush.filterNot { it.isOnServer() }.forEach { group ->
            uploadPhotoOf(group)
                // Why: its row named the refused photo, so the next round pushes it again without one.
                ?.onSuccess { if (it == PhotoUpload.REFUSED) heldGroupIds += group.id }
                ?.onFailure {
                    heldGroupIds += group.id
                    photoErrors += it
                }
        }
        // Why: before the deletes, so one that fails doesn't push again what the server already took.
        pendingGroups.filterNot { it.id in heldGroupIds }.forEach {
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
        photoErrors.firstOrNull()?.let { throw it }
    }

    // Why: a group with an invite code went up once, as GroupDao.getGroupIdsOnServer counts it.
    private fun GroupEntity.isOnServer(): Boolean = inviteCode != null

    // Why: null for a group with no photo waiting; once it is up, the device keeps the URL its row goes up with.
    private suspend fun uploadPhotoOf(group: GroupEntity): Result<PhotoUpload>? {
        val jpeg = group.pendingPhoto ?: return null
        return suspendRunCatching {
            val isUploaded = photoRemote.upload(
                groupId = group.id,
                jpeg = jpeg,
            )
            if (isUploaded) {
                local.markGroupPhotoUploaded(
                    id = group.id,
                    jpeg = jpeg,
                    photoUrl = pendingPhotoUrlOf(group),
                )
                PhotoUpload.UPLOADED
            } else {
                local.dropRefusedGroupPhoto(
                    id = group.id,
                    jpeg = jpeg,
                )
                PhotoUpload.REFUSED
            }
        }
    }

    // Why: a photo still to upload goes up under this change's version, which keeps the caches from the old one.
    private fun photoUrlOf(group: GroupEntity): String? = if (group.pendingPhoto != null) {
        pendingPhotoUrlOf(group)
    } else {
        group.photoUrl
    }

    private fun pendingPhotoUrlOf(group: GroupEntity): String = photoRemote.urlOf(
        groupId = group.id,
        version = group.updatedAt,
    )

    /*
     * Why: like the payment keys, another user's groups never go up as this account's, even ones not pushed yet:
     * the account they belong to has them on the server. Groups made before any account are this user's.
     */
    override suspend fun downloadGroups(userId: String): Result<Unit> = suspendRunCatching {
        val (remoteGroups, remoteMembers, remotePhotos) = coroutineScope {
            val groups = async { remote.fetchGroups() }
            val members = async { remote.fetchMembers() }
            // Why: only a nicety, so a failed read keeps the photos the device had and lets the sync go on.
            val photos = async { suspendRunCatching { remote.fetchMemberPhotos() }.getOrNull() }
            Triple(groups.await(), members.await(), photos.await())
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
        /*
         * Why: with every group download, so a photo someone changed reaches the faces on the next sync; its URL
         * changes with each upload, which keeps the image caches from showing the old one.
         */
        remotePhotos?.let { photos -> local.replaceMemberPhotos(photos.mapNotNull(mapper::toEntity)) }
    }

    override suspend fun fetchFirstDownloadGroupIds(): Result<List<String>> = suspendRunCatching {
        remote.fetchFirstDownloadGroupIds()
    }

    override suspend fun fetchGroupDownload(
        userId: String,
        groupId: String,
    ): Result<suspend () -> Unit> = suspendRunCatching {
        val (group, members) = coroutineScope {
            val group = async { remote.fetchGroup(groupId) }
            val members = async { remote.fetchMembers(groupId) }
            group.await() to members.await()
        }
        return@suspendRunCatching {
            // Why: a group gone since the server listed it brings nothing; its members would have no group to go in.
            if (group != null) {
                local.applyRemoteGroup(mapper.toEntity(group, ownerUserId = userId))
                members.forEach { dto -> local.applyRemoteMember(mapper.toEntity(dto, userId = userId)) }
            }
        }
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
