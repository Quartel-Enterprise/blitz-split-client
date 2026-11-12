package com.quare.blitzsplit.core.groups.data.mapper

import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.data.dto.MemberPhotoDto
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.SimplifyDebtsChange
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import com.quare.blitzsplit.core.provider.room.entity.MemberPhotoEntity

interface GroupMapper {
    /*
     * Why: each group with its own members, in the order the tables keep them, without the ones removed here. Each
     * member who joined gets their account's photo, and this device's user the profile's, which may be newer.
     */
    fun toDomain(
        groups: List<GroupEntity>,
        members: List<GroupMemberEntity>,
        photos: List<MemberPhotoEntity>,
        myPhotoUrl: String?,
    ): List<Group>

    fun toEntity(dto: MemberPhotoDto): MemberPhotoEntity?

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

    // Why: photoUrl is the one the row goes up with, a photo still to upload's included.
    fun toDto(
        entity: GroupEntity,
        userId: String,
        photoUrl: String?,
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

    // Why: the JSON Room keeps a change of "Simplificar dívidas" in until it goes up, as the server reads it.
    fun encode(change: SimplifyDebtsChange): String
}
