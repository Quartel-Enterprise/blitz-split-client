package com.quare.blitzsplit.core.model

/*
 * Why: syncs run silently (Pierre's call, beta feedback), so "Syncing…" only shows during the account's first full
 * sync and for the retry after a failed one, until it settles. wasSyncingOrFailed is what the screen showed just
 * before: a retry after FAILED, or the same sync going on. Home, Groups and Profile all ask this one rule.
 */
fun showsSyncing(
    awaitsFullSync: Boolean,
    wasSyncingOrFailed: Boolean,
): Boolean = awaitsFullSync || wasSyncingOrFailed
