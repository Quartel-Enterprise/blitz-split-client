package com.quare.blitzsplit.feature.groups.presentation.model

import com.quare.blitzsplit.core.model.SyncState

// Why: the sync's state, and whether it is the account's first full sync, the one sync the badge says "Syncing…" for.
internal data class SyncActivity(
    val state: SyncState,
    val isFirstFullSync: Boolean,
)
