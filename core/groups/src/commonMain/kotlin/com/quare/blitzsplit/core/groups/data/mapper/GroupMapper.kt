package com.quare.blitzsplit.core.groups.data.mapper

import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity

interface GroupMapper {
    // Why: each group with its own members, in the order the tables keep them.
    fun toDomain(
        groups: List<GroupEntity>,
        members: List<GroupMemberEntity>,
    ): List<Group>

    // Why: a new group, pending until the sync. With no owner, it is kept by whoever the first sync runs under.
    fun toEntity(
        group: Group,
        createdAt: Long,
        ownerUserId: String?,
    ): GroupEntity

    // Why: the creator's member is the owner's account; the others have none until they join.
    fun toMemberEntities(
        group: Group,
        createdAt: Long,
        ownerUserId: String?,
    ): List<GroupMemberEntity>

    fun toDto(
        entity: GroupEntity,
        userId: String,
    ): GroupDto

    fun toDto(entity: GroupMemberEntity): GroupMemberDto

    fun toEntity(
        dto: GroupDto,
        ownerUserId: String,
    ): GroupEntity

    fun toEntity(
        dto: GroupMemberDto,
        userId: String,
    ): GroupMemberEntity
}
