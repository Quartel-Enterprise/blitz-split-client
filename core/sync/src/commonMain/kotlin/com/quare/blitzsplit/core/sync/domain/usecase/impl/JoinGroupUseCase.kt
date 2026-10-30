package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.groups.domain.model.MemberColors
import com.quare.blitzsplit.core.groups.domain.model.MemberNameRules
import com.quare.blitzsplit.core.groups.domain.repository.InviteRepository
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.UserProfile
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.sync.SyncCoordinator
import com.quare.blitzsplit.core.sync.domain.usecase.JoinGroup
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.first

/*
 * Why: joining needs an account on the server, so someone without one gets an anonymous one first, as the sync would.
 * Someone with no profile on the device yet gets one from the group: the name they joined as and the member's color,
 * so they skip "Como você quer aparecer?". Each person keeps their profile's color in every group they join.
 */
class JoinGroupUseCase(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val inviteRepository: InviteRepository,
    private val coordinator: SyncCoordinator,
) : JoinGroup {
    override suspend fun invoke(
        invite: Invite,
        choice: JoinChoice,
    ): Result<JoinOutcome> = suspendRunCatching {
        val profile = profileRepository.observeProfile().first()
        val avatarColor = profile?.avatarColor ?: colorOf(invite, choice)
        // Why: a member named before names needed 2 letters can't become a profile, so it isn't joined at all.
        if (profile ==
            null
        ) {
            require(MemberNameRules.isLongEnough(nameOf(invite, choice))) { "Name too short for a profile" }
        }
        accountRepository.ensureRegistered().getOrThrow()
        val outcome = inviteRepository
            .joinGroup(
                code = invite.code,
                choice = choice,
                avatarColor = avatarColor,
            ).getOrThrow()
        if (outcome is JoinOutcome.Joined || outcome is JoinOutcome.AlreadyMember) {
            if (profile == null) {
                profileRepository.saveProfile(
                    UserProfile(
                        displayName = nameOf(invite, choice),
                        avatarColor = avatarColor,
                        photo = null,
                    ),
                )
            }
            coordinator.syncNow().getOrThrow()
        }
        outcome
    }

    private fun colorOf(
        invite: Invite,
        choice: JoinChoice,
    ): AvatarColor = when (choice) {
        is JoinChoice.AsMember -> invite.members.firstOrNull { it.id == choice.memberId }?.avatarColor
        is JoinChoice.AsNewMember -> null
    } ?: MemberColors.findLeastUsed(invite.members.map { it.avatarColor })

    private fun nameOf(
        invite: Invite,
        choice: JoinChoice,
    ): String = when (choice) {
        is JoinChoice.AsMember -> invite.members.first { it.id == choice.memberId }.name
        is JoinChoice.AsNewMember -> choice.name
    }.trim()
}
