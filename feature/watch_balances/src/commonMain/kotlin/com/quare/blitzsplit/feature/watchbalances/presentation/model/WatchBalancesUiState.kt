package com.quare.blitzsplit.feature.watchbalances.presentation.model

data class WatchBalancesUiState(
    // Why: false until the balances came from Room and no first download is still filling the watch.
    val isLoaded: Boolean,
    // Why: one per currency, the one with the most groups first.
    val totals: List<WatchTotal>,
    // Why: the largest balance first, whichever its direction; a group with no open pair with you never shows.
    val groups: List<WatchGroupRow>,
    val isOnline: Boolean,
    // Why: "1 change queued": payments recorded on the watch not up yet.
    val queuedChangeCount: Int,
    // Why: "Updated at 14:32", the watch's last sync in epoch millis; null until one went through.
    val lastSyncedAt: Long?,
) {
    // Why: 15.3b "Nothing open": you owe nothing and nobody owes you.
    val isAllSettled: Boolean
        get() = isLoaded && groups.isEmpty()

    // Why: "Offline · 1 change queued" on top (15.6b); offline with nothing queued says only "Offline".
    val showsOfflineNotice: Boolean
        get() = !isOnline
}
