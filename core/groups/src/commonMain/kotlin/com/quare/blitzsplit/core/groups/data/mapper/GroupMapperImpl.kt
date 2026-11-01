package com.quare.blitzsplit.core.groups.data.mapper

import com.quare.blitzsplit.core.groups.data.dto.CategorySplitDto
import com.quare.blitzsplit.core.groups.data.dto.DefaultSplitDto
import com.quare.blitzsplit.core.groups.data.dto.DefaultSplitWeightDto
import com.quare.blitzsplit.core.groups.data.dto.GroupDto
import com.quare.blitzsplit.core.groups.data.dto.GroupMemberDto
import com.quare.blitzsplit.core.groups.domain.model.CategorySplit
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.provider.room.entity.GroupEntity
import com.quare.blitzsplit.core.provider.room.entity.GroupMemberEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/*
 * Why: a type, color or currency a newer app wrote reads as the closest one this version knows, so nobody loses
 * sight of a group because of it.
 */
class GroupMapperImpl : GroupMapper {
    private val json = Json {
        ignoreUnknownKeys = true
        // Why: an older version saved no "exceptions"; the absent key reads as none.
        explicitNulls = false
    }

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
                // Why: both are null for a group made here before any account, which the first sync gives this one.
                isCreatedByMe = group.createdBy == group.ownerUserId,
                defaultSplit = defaultSplitOf(group.defaultSplit),
                updatedAt = group.updatedAt,
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
        defaultSplit = group.defaultSplit?.let {
            encode(
                defaultSplit = it,
                over = null,
            )
        },
        splitIncomes = null,
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
        defaultSplit = objectOrNull(entity.defaultSplit),
    )

    /*
     * Why: only this device's own account goes up. The server keeps the account of a member someone joined as anyway,
     * and refuses one the member no longer has, as after "Liberar nome", with the whole upload.
     */
    override fun toDto(entity: GroupMemberEntity): GroupMemberDto = GroupMemberDto(
        id = entity.id,
        groupId = entity.groupId,
        userId = entity.userId.takeIf { entity.isMe },
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
        defaultSplit = dto.defaultSplit?.toString(),
        // Why: never on the server; applyRemoteGroup leaves the ones this device has alone.
        splitIncomes = null,
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
        userId = entity.userId,
        inviteCode = entity.inviteCode,
    )

    override fun encode(
        defaultSplit: GroupDefaultSplit,
        over: String?,
    ): String {
        val known = json.encodeToJsonElement(
            DefaultSplitDto(
                mode = defaultSplit.mode.code,
                weights = weightDtosOf(defaultSplit.weights),
                exceptions = defaultSplit.exceptions.map {
                    CategorySplitDto(
                        categoryId = it.categoryId,
                        mode = it.mode.code,
                        weights = weightDtosOf(it.weights),
                    )
                },
            ),
        )
        return JsonObject(objectOrNull(over).orEmpty() + known.jsonObject).toString()
    }

    override fun encodeIncomes(incomes: Map<String, Long>): String? = incomes.takeIf { it.isNotEmpty() }?.let {
        json.encodeToString(it)
    }

    override fun decodeIncomes(text: String?): Map<String, Long> = text
        ?.let { runCatching { json.decodeFromString<Map<String, Long>>(it) }.getOrNull() }
        .orEmpty()

    // Why: a split a newer app wrote in a mode this version doesn't know splits equally, as a group without one.
    private fun defaultSplitOf(text: String?): GroupDefaultSplit? {
        val dto = decodeOrNull(text) ?: return null
        val mode = DefaultSplitMode.fromCodeOrNull(dto.mode) ?: return null
        return GroupDefaultSplit(
            mode = mode,
            weights = dto.weights.associate { it.memberId to it.weight },
            // Why: an exception in a mode this version doesn't offer is left out, its bills splitting by the default.
            exceptions = dto.exceptions.orEmpty().mapNotNull { exception ->
                val exceptionMode = DefaultSplitMode
                    .fromCodeOrNull(exception.mode)
                    ?.takeIf { it in CategorySplit.modes }
                    ?: return@mapNotNull null
                CategorySplit(
                    categoryId = exception.categoryId,
                    mode = exceptionMode,
                    weights = exception.weights.associate { it.memberId to it.weight },
                )
            },
        )
    }

    private fun weightDtosOf(weights: Map<String, Long>): List<DefaultSplitWeightDto> =
        weights.map { (memberId, weight) ->
            DefaultSplitWeightDto(
                memberId = memberId,
                weight = weight,
            )
        }

    private fun decodeOrNull(text: String?): DefaultSplitDto? = text?.let {
        runCatching { json.decodeFromString<DefaultSplitDto>(it) }.getOrNull()
    }

    private fun objectOrNull(text: String?): JsonObject? = text?.let {
        runCatching { json.parseToJsonElement(it) as? JsonObject }.getOrNull()
    }
}
