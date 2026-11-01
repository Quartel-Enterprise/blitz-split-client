package com.quare.blitzsplit.core.contacts.domain.usecase.impl

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.usecase.AddContactToGroup
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.MemberColors
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.AddMember
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first
import kotlin.uuid.Uuid

class AddContactToGroupUseCase(
    private val repository: GroupRepository,
    private val observeGroup: ObserveGroup,
    private val addMember: AddMember,
    private val currentTimeMillis: CurrentTimeMillis,
) : AddContactToGroup {
    override suspend fun invoke(
        contact: Contact,
        groupId: String,
    ): Result<Unit> {
        val userId = contact.userId ?: return addMember(groupId, contact.name)
        val group = observeGroup(groupId).first()
            ?: return Result.failure(NoSuchElementException("No group $groupId"))
        // Why: a stale tap never adds someone the group already has.
        if (group.members.any { it.userId == userId }) return Result.success(Unit)
        return repository.addMember(
            groupId = groupId,
            member = GroupMember(
                id = Uuid.random().toString(),
                name = GroupNameRules.toCapped(contact.name.trim()),
                avatarColor = contact.color.takeUnless { color -> group.members.any { it.avatarColor == color } }
                    ?: MemberColors.findLeastUsed(group.members.map(GroupMember::avatarColor)),
                isMe = false,
                hasJoined = true,
                userId = userId,
                inviteCode = null,
            ),
            addedAt = currentTimeMillis(),
        )
    }
}
