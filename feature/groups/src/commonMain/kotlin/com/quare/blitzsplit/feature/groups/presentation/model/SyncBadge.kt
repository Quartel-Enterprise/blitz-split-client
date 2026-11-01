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
}

// Why: Grupos' and Início's chip alike. Online, a queued change is about to go, since the coordinator syncs at once.
internal fun syncBadgeOf(
    syncState: SyncState,
    isOnline: Boolean,
): SyncBadge? = when {
    syncState == SyncState.SYNCED -> null
    !isOnline -> SyncBadge.OFFLINE
    syncState == SyncState.FAILED -> SyncBadge.FAILED
    else -> SyncBadge.SYNCING
}
