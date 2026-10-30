package com.quare.blitzsplit.core.groups.data.mapper

import com.quare.blitzsplit.core.groups.data.dto.InviteDto
import com.quare.blitzsplit.core.groups.data.dto.InviteMemberDto
import com.quare.blitzsplit.core.groups.data.dto.JoinGroupResultDto
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteMember
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType

// Why: like the groups, a type, color or currency a newer app wrote reads as the closest one this version knows.
class InviteMapperImpl : InviteMapper {
    override fun toDomain(
        code: String,
        dto: InviteDto,
    ): Invite = Invite(
        code = code,
        groupId = dto.groupId,
        groupName = dto.name,
        groupType = GroupType.fromCodeOrNull(dto.type) ?: GroupType.OTHER,
        groupColor = AvatarColor.fromCodeOrNull(dto.color) ?: AvatarColor.default,
        currency = GroupCurrency.fromCodeOrNull(dto.currency) ?: GroupCurrency.fallback,
        creatorName = dto.creatorName,
        isMember = dto.isMember,
        members = dto.members.map(::toDomain),
    )

    // Why: a status this version doesn't know can only come from a newer server, which a retry can't fix either.
    override fun toDomain(dto: JoinGroupResultDto): JoinOutcome = when (dto.status) {
        JOINED -> JoinOutcome.Joined(requireNotNull(dto.groupId))
        ALREADY_MEMBER -> JoinOutcome.AlreadyMember(requireNotNull(dto.groupId))
        TAKEN -> JoinOutcome.MemberTaken
        MEMBER_GONE -> JoinOutcome.MemberGone
        NAME_TAKEN -> JoinOutcome.NameTaken
        else -> JoinOutcome.InvalidLink
    }

    private fun toDomain(dto: InviteMemberDto): InviteMember = InviteMember(
        id = dto.id,
        name = dto.name,
        avatarColor = AvatarColor.fromCodeOrNull(dto.avatarColor) ?: AvatarColor.default,
        hasJoined = dto.hasJoined,
    )

    private companion object {
        const val JOINED = "joined"
        const val ALREADY_MEMBER = "already_member"
        const val TAKEN = "taken"
        const val MEMBER_GONE = "member_gone"
        const val NAME_TAKEN = "name_taken"
    }
}
