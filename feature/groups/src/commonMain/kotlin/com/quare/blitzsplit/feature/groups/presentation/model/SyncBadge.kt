package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/*
 * Why: everything synced draws no badge at all; these are the states that ask for attention.
 * OFFLINE means something waits on the device for the internet ("Offline · 1 alteração na fila"), and FAILED is
 * tapped to retry. SYNCING only shows during the account's first full sync (Pierre's call, beta feedback): the app is
 * offline first, so every later sync runs silently.
 */
enum class SyncBadge {
    OFFLINE,
    SYNCING,
    FAILED,
    ;

    companion object {
        internal fun ofOrNull(
            sync: SyncActivity,
            isOnline: Boolean,
        ): SyncBadge? = when {
            sync.state == SyncState.SYNCED -> null
            !isOnline -> OFFLINE
            sync.state == SyncState.FAILED -> FAILED
            sync.isFirstFullSync -> SYNCING
            else -> null
        }

        // Why: a first download is pending while the account's copy is partial (a sign in, a new device, a relaunch).
        internal fun observeActivity(
            syncState: Flow<SyncState>,
            firstDownload: Flow<FirstDownloadProgress?>,
        ): Flow<SyncActivity> = combine(
            syncState,
            firstDownload.map { it != null }.distinctUntilChanged(),
            ::SyncActivity,
        )
    }
}
