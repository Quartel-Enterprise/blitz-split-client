package com.quare.blitzsplit.feature.groups.presentation.model

/*
 * Why: everything synced draws no badge at all; these are the states that ask for attention.
 * OFFLINE means something waits on the device for the internet, and FAILED is tapped to retry.
 */
enum class SyncBadge {
    OFFLINE,
    SYNCING,
    FAILED,
}
