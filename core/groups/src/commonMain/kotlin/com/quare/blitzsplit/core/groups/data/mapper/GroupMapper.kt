package com.quare.blitzsplit.core.groups.data.mapper

import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity

interface GroupMapper {
    // Why: each group with its own members, in the order the tables keep them, without the ones removed here.
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

    // Why: a member added to a group the device has, pending until the sync.
    fun toMemberEntity(
        groupId: String,
        member: GroupMember,
        addedAt: Long,
    ): GroupMemberEntity

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

    /*
     * Why: the JSON Room keeps "Divisão padrão" in, the same the server keeps. Only the keys this version knows are
     * written over the stored JSON, so the ones a newer app added to it survive the change.
     */
    fun encode(
        defaultSplit: GroupDefaultSplit,
        over: String?,
    ): String

    // Why: the incomes "Pela renda" was typed with, which only this device keeps; null when there are none.
    fun encodeIncomes(incomes: Map<String, Long>): String?

    // Why: empty for none, or for what this version can't read.
    fun decodeIncomes(text: String?): Map<String, Long>
}
