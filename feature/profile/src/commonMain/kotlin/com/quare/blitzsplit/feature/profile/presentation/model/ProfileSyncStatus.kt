package com.quare.blitzsplit.feature.profile.presentation.model

/*
 * Why: everything synced draws no sync row at all; these are the states that ask for attention.
 * WAITING means something queued on the device waits for the internet, and FAILED is tapped to retry.
 */
enum class ProfileSyncStatus {
    WAITING,
    SYNCING,
    FAILED,
}
