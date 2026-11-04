package com.quare.blitzsplit.core.groups.domain.usecase

// Why: "Ana desligou depois de você" (13.1e) shows once, on whichever screen of the group is on top first.
fun interface ConsumeSimplifyDebtsConflict {
    suspend operator fun invoke(groupId: String): Result<Unit>
}
