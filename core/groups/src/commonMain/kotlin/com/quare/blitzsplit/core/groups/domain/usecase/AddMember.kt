package com.quare.blitzsplit.core.groups.domain.usecase

fun interface AddMember {
    // Why: someone who hasn't joined, at the end of the list, in the color fewest members of the group have.
    suspend operator fun invoke(
        groupId: String,
        name: String,
    ): Result<Unit>
}
