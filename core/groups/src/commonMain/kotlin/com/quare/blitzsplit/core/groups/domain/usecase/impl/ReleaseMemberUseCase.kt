package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.ReleaseMember

class ReleaseMemberUseCase(
    private val repository: GroupRepository,
) : ReleaseMember {
    override suspend fun invoke(memberId: String): Result<Unit> = repository.requestMemberRelease(memberId)
}
