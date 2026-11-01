package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.SyncState

/*
 * Why: everything synced draws no badge at all; these are the states that ask for attention.
 * OFFLINE means something waits on the device for the internet ("Offline · 1 alteração na fila"), and FAILED is
 * tapped to retry.
 */
enum class SyncBadge {
    OFFLINE,
    SYNCING,
    FAILED,
    ;

    companion object {
        // Why: online, a queued change is about to go, since the coordinator syncs as soon as one is queued.
        fun ofOrNull(
            syncState: SyncState,
            isOnline: Boolean,
        ): SyncBadge? = when {
            syncState == SyncState.SYNCED -> null
            !isOnline -> OFFLINE
            syncState == SyncState.FAILED -> FAILED
            else -> SYNCING
        }
    }
}
