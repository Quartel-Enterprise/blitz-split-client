package com.quare.blitzsplit.core.groups.testing

import com.quare.blitzsplit.core.groups.domain.model.Invite
import com.quare.blitzsplit.core.groups.domain.model.InviteTarget
import com.quare.blitzsplit.core.groups.domain.model.JoinChoice
import com.quare.blitzsplit.core.groups.domain.model.JoinOutcome
import com.quare.blitzsplit.core.groups.domain.repository.InviteRepository
import com.quare.blitzsplit.core.model.AvatarColor
import kotlinx.coroutines.CompletableDeferred

// In-memory InviteRepository: the answers and the failure knobs are public vars, and every call is recorded.
class FakeInviteRepository(
    var invite: Invite? = null,
    var joinOutcome: JoinOutcome = JoinOutcome.InvalidLink,
) : InviteRepository {
    var fetchError: Throwable? = null
    var joinError: Throwable? = null
    val fetchedCodes = mutableListOf<String>()
    val fetchedTargets = mutableListOf<InviteTarget>()
    val joins = mutableListOf<Join>()

    // Why: while set, a fetch waits for it, so a test can look at the screen while the invite is on its way.
    var fetchGate: CompletableDeferred<Unit>? = null

    // Why: while set, a join waits for it, so a test can act while the join is in flight.
    var joinGate: CompletableDeferred<Unit>? = null

    data class Join(
        val code: String,
        val choice: JoinChoice,
        val avatarColor: AvatarColor,
    )

    override suspend fun fetchInvite(target: InviteTarget): Result<Invite?> {
        fetchedCodes += target.code
        fetchedTargets += target
        fetchGate?.await()
        fetchError?.let { return Result.failure(it) }
        return Result.success(invite)
    }

    override suspend fun joinGroup(
        code: String,
        choice: JoinChoice,
        avatarColor: AvatarColor,
    ): Result<JoinOutcome> {
        joinGate?.await()
        joins += Join(code, choice, avatarColor)
        joinError?.let { return Result.failure(it) }
        return Result.success(joinOutcome)
    }
}
