package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.GroupExitOutcome
import com.quare.blitzsplit.core.sync.SyncCoordinator
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/*
 * Why: "Sair do grupo" and "Apagar grupo" alike. The device pushes what it has first, so the server judges the
 * balances with every bill and payment made here, and asks under the same sync lock, so no other sync can bring the
 * group back while the server takes it. Not cancelled with the screen (a swipe back mid request), so a group the
 * server took leaves the device too. A balance the server knows of and the device doesn't came from a sync since:
 * one more brings it here.
 */
internal suspend fun SyncCoordinator.runGroupExit(
    ask: suspend () -> Result<GroupExitOutcome>,
): Result<GroupExitOutcome> = withContext(NonCancellable) {
    syncThen(ask).onSuccess { outcome ->
        if (outcome == GroupExitOutcome.HAS_BALANCE) syncNow()
    }
}
