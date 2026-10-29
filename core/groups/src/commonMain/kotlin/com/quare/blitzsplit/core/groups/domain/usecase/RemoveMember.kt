package com.quare.blitzsplit.core.groups.domain.usecase

fun interface RemoveMember {
    // Why: only someone who hasn't joined; a member who joined, or this device's user, stays.
    suspend operator fun invoke(memberId: String): Result<Unit>
}
