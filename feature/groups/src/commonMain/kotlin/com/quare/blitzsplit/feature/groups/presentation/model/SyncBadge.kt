package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.showsSyncing

/*
 * Why: everything synced draws no badge at all; these are the states that ask for attention.
 * OFFLINE means something waits on the device for the internet ("Offline · 1 alteração na fila"), and FAILED is
 * tapped to retry. SYNCING only shows during the account's first full sync and for the sync after a failure, the retry
 * (Pierre's call, beta feedback): the app is offline first, so every other sync runs silently. Never while the screen
 * already shows the account's groups (Pierre's call, beta feedback): with data on it, a sync is nothing to wait for.
 */
enum class SyncBadge {
    OFFLINE,
    SYNCING,
    FAILED,
    ;

    companion object {
        /*
         * Why: awaitsFullSync holds from a restore until a sync gets the whole copy through, retries included. previous
         * is the badge shown, so a retry after FAILED says "Syncing…" until it settles, synced or failed again.
         * showsData is whether the screen shows any group.
         */
        internal fun ofOrNull(
            syncState: SyncState,
            isOnline: Boolean,
            awaitsFullSync: Boolean,
            previous: SyncBadge?,
            showsData: Boolean,
        ): SyncBadge? = when {
            syncState == SyncState.SYNCED -> null
            !isOnline -> OFFLINE
            syncState == SyncState.FAILED -> FAILED
            showsData -> null
            showsSyncing(awaitsFullSync, wasSyncingOrFailed = previous == FAILED || previous == SYNCING) -> SYNCING
            else -> null
        }

        // Why: the groups came while "Syncing…" showed, which goes with them, never drawn over the account's data.
        internal fun SyncBadge?.unlessSyncingOver(showsData: Boolean): SyncBadge? =
            takeUnless { it == SYNCING && showsData }
    }
}
