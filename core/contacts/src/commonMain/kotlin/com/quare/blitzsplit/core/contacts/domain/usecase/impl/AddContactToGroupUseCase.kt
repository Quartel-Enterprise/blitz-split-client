package com.quare.blitzsplit.core.contacts.domain.usecase.impl

import com.quare.blitzsplit.core.contacts.domain.model.Contact
import com.quare.blitzsplit.core.contacts.domain.model.MemberNameTakenException
import com.quare.blitzsplit.core.contacts.domain.usecase.AddContactToGroup
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.groups.domain.model.GroupMember
import com.quare.blitzsplit.core.groups.domain.model.GroupNameRules
import com.quare.blitzsplit.core.groups.domain.model.MemberColors
import com.quare.blitzsplit.core.groups.domain.model.MemberNameCheck
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.AddMember
import com.quare.blitzsplit.core.groups.domain.usecase.CheckMemberName
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroup
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import kotlinx.coroutines.flow.first
import kotlin.uuid.Uuid

class AddContactToGroupUseCase(
    private val repository: GroupRepository,
    private val observeGroup: ObserveGroup,
    private val addMember: AddMember,
    private val checkMemberName: CheckMemberName,
    private val currentTimeMillis: CurrentTimeMillis,
) : AddContactToGroup {
    override suspend fun invoke(
        contact: Contact,
        groupId: String,
    ): Result<Unit> {
        val group = observeGroup(groupId).first()
            ?: return Result.failure(NoSuchElementException("No group $groupId"))
        // Why: a stale tap never adds someone the group already has, a guest being their name there.
        if (group.members.any { PeopleBalances.keyOf(it) == contact.key }) return Result.success(Unit)
        /*
         * Why: two members of a group never share a name. Someone with an account takes the nickname ("Ana 2"); a
         * guest is their name, so another member already called that leaves them out of this group.
         */
        val check = checkMemberName(
            name = contact.name,
            takenNames = group.members.map(GroupMember::name),
            adderName = group.members
                .firstOrNull { it.isMe }
                ?.name
                .orEmpty(),
        )
        val name = when (check) {
            is MemberNameCheck.Available -> check.name
            is MemberNameCheck.Taken -> check.suggestions.lastOrNull()?.takeIf { contact.userId != null }
            MemberNameCheck.Blank, MemberNameCheck.TooShort -> null
        } ?: return Result.failure(MemberNameTakenException(contact.name))
        val userId = contact.userId ?: return addMember(groupId, name)
        return repository.addMember(
            groupId = groupId,
            member = GroupMember(
                id = Uuid.random().toString(),
                name = GroupNameRules.toCapped(name),
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
