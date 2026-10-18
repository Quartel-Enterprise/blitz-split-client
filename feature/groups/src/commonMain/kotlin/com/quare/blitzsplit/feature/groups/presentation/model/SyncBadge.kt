package com.quare.blitzsplit.feature.groups.presentation.model

enum class SyncBadge {
    OFFLINE,

    // Why: online is not enough to say "synced": the first sync of this launch may still be running.
    SYNCING,

    SYNCED,
}
