package com.quare.blitzsplit.core.model

/*
 * Why: SYNCED is the quiet state; the UIs only draw the others, which ask for attention. PENDING is
 * a local change waiting for the next sync, FAILED a last sync that got an error or no answer.
 */
enum class SyncState {
    SYNCED,
    PENDING,
    SYNCING,
    FAILED,
}
