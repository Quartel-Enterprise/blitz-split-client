package com.quare.blitzsplit.core.sync.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome

fun interface DeleteGroup {
    /*
     * Why: "Apagar grupo", for everyone and online only. The device pushes what it has first, so the server judges
     * the balances with every bill and payment made here; a failure, the sync's included, changes nothing.
     */
    suspend operator fun invoke(groupId: String): Result<GroupExitOutcome>
}
