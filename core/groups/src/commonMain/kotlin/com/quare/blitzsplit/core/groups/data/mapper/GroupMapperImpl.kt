package com.quare.blitzsplit.core.groups.data.mapper

import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity

/*
 * Why: a type, color or currency a newer app wrote reads as the closest one this version knows, so nobody loses
 * sight of a group because of it.
 */
class GroupMapperImpl : GroupMapper {
    override fun toDomain(
        groups: List<GroupEntity>,
        members: List<GroupMemberEntity>,
    ): List<Group> {
        val membersByGroup = members.groupBy { it.groupId }
        return groups.map { group ->
            val ofGroup = membersByGroup[group.id].orEmpty()
            // Why: a removal not up yet still leaves the group pending.
            Group(
                id = group.id,
                name = group.name,
                type = GroupType.fromCodeOrNull(group.type) ?: GroupType.OTHER,
                color = AvatarColor.fromCodeOrNull(group.color) ?: AvatarColor.default,
                currency = GroupCurrency.fromCodeOrNull(group.currency) ?: GroupCurrency.fallback,
                members = ofGroup.filterNot { it.isRemoved }.map(::toDomain),
                isPending = group.pendingSync || ofGroup.any { it.pendingSync },
                inviteCode = group.inviteCode,
            )
        }
    }

    override fun toEntity(
        group: Group,
        createdAt: Long,
        ownerUserId: String?,
    ): GroupEntity = GroupEntity(
        id = group.id,
        name = group.name,
        type = group.type.code,
        color = group.color.code,
        currency = group.currency.code,
        createdBy = ownerUserId,
        addedAt = createdAt,
        updatedAt = createdAt,
        pendingSync = true,
        ownerUserId = ownerUserId,
        inviteCode = group.inviteCode,
    )

    // Why: one write can't tell the members apart by the clock, so each one's millisecond keeps the order given.
    override fun toMemberEntities(
        group: Group,
        createdAt: Long,
        ownerUserId: String?,
    ): List<GroupMemberEntity> = group.members.mapIndexed { index, member ->
        GroupMemberEntity(
            id = member.id,
            groupId = group.id,
            name = member.name,
            avatarColor = member.avatarColor.code,
            userId = ownerUserId.takeIf { member.isMe },
            isMe = member.isMe,
            addedAt = createdAt + index,
            updatedAt = createdAt,
            pendingSync = true,
            isRemoved = false,
            inviteCode = null,
        )
    }

    override fun toMemberEntity(
        groupId: String,
        member: GroupMember,
        addedAt: Long,
    ): GroupMemberEntity = GroupMemberEntity(
        id = member.id,
        groupId = groupId,
        name = member.name,
        avatarColor = member.avatarColor.code,
        userId = null,
        isMe = false,
        addedAt = addedAt,
        updatedAt = addedAt,
        pendingSync = true,
        isRemoved = false,
        inviteCode = null,
    )

    override fun toDto(
        entity: GroupEntity,
        userId: String,
    ): GroupDto = GroupDto(
        id = entity.id,
        createdBy = entity.createdBy ?: userId,
        name = entity.name,
        type = entity.type,
        color = entity.color,
        currency = entity.currency,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
        inviteCode = null,
    )

    override fun toDto(entity: GroupMemberEntity): GroupMemberDto = GroupMemberDto(
        id = entity.id,
        groupId = entity.groupId,
        userId = entity.userId,
        name = entity.name,
        avatarColor = entity.avatarColor,
        addedAt = entity.addedAt,
        updatedAt = entity.updatedAt,
        inviteCode = null,
    )

    override fun toEntity(
        dto: GroupDto,
        ownerUserId: String,
    ): GroupEntity = GroupEntity(
        id = dto.id,
        name = dto.name,
        type = dto.type,
        color = dto.color,
        currency = dto.currency,
        createdBy = dto.createdBy,
        addedAt = dto.addedAt,
        updatedAt = dto.updatedAt,
        pendingSync = false,
        ownerUserId = ownerUserId,
        inviteCode = dto.inviteCode,
    )

    override fun toEntity(
        dto: GroupMemberDto,
        userId: String,
    ): GroupMemberEntity = GroupMemberEntity(
        id = dto.id,
        groupId = dto.groupId,
        name = dto.name,
        avatarColor = dto.avatarColor,
        userId = dto.userId,
        isMe = dto.userId == userId,
        addedAt = dto.addedAt,
        updatedAt = dto.updatedAt,
        pendingSync = false,
        isRemoved = false,
        inviteCode = dto.inviteCode,
    )

    private fun toDomain(entity: GroupMemberEntity): GroupMember = GroupMember(
        id = entity.id,
        name = entity.name,
        avatarColor = AvatarColor.fromCodeOrNull(entity.avatarColor) ?: AvatarColor.default,
        isMe = entity.isMe,
        hasJoined = entity.isMe || entity.userId != null,
        inviteCode = entity.inviteCode,
    )
}
