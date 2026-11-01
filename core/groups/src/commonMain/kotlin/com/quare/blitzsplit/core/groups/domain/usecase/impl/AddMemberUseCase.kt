package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.MemberColors
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.AddMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first
import kotlin.uuid.Uuid

// Why: like a new group, the member exists on the device right away, offline included; the sync takes it up.
class AddMemberUseCase(
    private val repository: GroupRepository,
    private val observeGroup: ObserveGroup,
    private val currentTimeMillis: CurrentTimeMillis,
) : AddMember {
    override suspend fun invoke(
        groupId: String,
        name: String,
    ): Result<Unit> {
        val group = observeGroup(groupId).first()
            ?: return Result.failure(NoSuchElementException("No group $groupId"))
        return repository.addMember(
            groupId = groupId,
            member = GroupMember(
                id = Uuid.random().toString(),
                name = GroupNameRules.toCapped(name.trim()),
                avatarColor = MemberColors.findLeastUsed(group.members.map(GroupMember::avatarColor)),
                isMe = false,
                hasJoined = false,
                userId = null,
                inviteCode = null,
            ),
            addedAt = currentTimeMillis(),
        )
    }
}
