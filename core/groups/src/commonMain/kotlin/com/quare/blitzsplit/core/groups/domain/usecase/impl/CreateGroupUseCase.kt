package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.NewGroupMember
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.CreateGroup
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupType
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlin.uuid.Uuid

// Why: the group exists on the device right away, offline included; the sync takes it to the server.
class CreateGroupUseCase(
    private val repository: GroupRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : CreateGroup {
    override suspend fun invoke(
        name: String,
        type: GroupType,
        color: AvatarColor,
        currency: GroupCurrency,
        members: List<NewGroupMember>,
    ): Result<String> {
        val id = Uuid.random().toString()
        return repository
            .createGroup(
                group = Group(
                    id = id,
                    name = GroupNameRules.toCapped(name.trim()),
                    type = type,
                    color = color,
                    currency = currency,
                    members = members.map { member ->
                        GroupMember(
                            id = Uuid.random().toString(),
                            name = GroupNameRules.toCapped(member.name.trim()),
                            avatarColor = member.avatarColor,
                            isMe = member.isMe,
                            hasJoined = member.isMe,
                            inviteCode = null,
                        )
                    },
                    isPending = true,
                    inviteCode = null,
                ),
                createdAt = currentTimeMillis(),
            ).map { id }
    }
}
