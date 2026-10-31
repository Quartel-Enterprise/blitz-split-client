package com.quare.blitzsplit.core.groups.domain.usecase

fun interface ReleaseMember {
    // Why: "Liberar nome", by the group's creator, online only: whoever joined as the member leaves the group.
    suspend operator fun invoke(memberId: String): Result<Unit>
}
