package com.quare.blitzsplit.feature.profile.presentation.viewmodel

import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.showsSyncing
import com.quare.blitzsplit.feature.profile.presentation.model.PendingChange
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileSyncStatus
import com.quare.blitzsplit.feature.profile.presentation.model.ProfileUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

// Why: "Account and sync"'s status and what still waits to go up, as each source answers.
internal fun MutableStateFlow<ProfileUiState>.observeSyncSummary(
    syncState: Flow<SyncState>,
    hasPendingProfile: Flow<Boolean>,
    hasPendingKeys: Flow<Boolean>,
    hasPendingPreferences: Flow<Boolean>,
    isOnline: Flow<Boolean>,
    awaitsFullSync: Flow<Boolean>,
): Flow<Unit> = combine(
    syncState,
    combine(hasPendingProfile, hasPendingKeys, hasPendingPreferences, ::Triple),
    isOnline,
    awaitsFullSync,
) { state, pending, online, awaitsFull ->
    val (pendingProfile, pendingKeys, pendingPreferences) = pending
    update {
        it.copy(
            isOnline = online,
            syncStatus = syncStatusOf(state, awaitsFull, online, it.syncStatus),
            pendingChanges = listOfNotNull(
                PendingChange.PROFILE.takeIf { pendingProfile },
                PendingChange.PAYMENT_KEYS.takeIf { pendingKeys },
                PendingChange.NOTIFICATION_PREFERENCES.takeIf { pendingPreferences },
            ),
        )
    }
}

/*
 * Why: SYNCED draws nothing; PENDING waits for the connection, or for the next sync, which is imminent online.
 * SYNCING follows `showsSyncing`, as Home's and Groups' chip does (previous is the status shown), and loses to being
 * offline there too; every other sync keeps a waiting row and otherwise shows the row at rest.
 */
private fun syncStatusOf(
    syncState: SyncState,
    awaitsFullSync: Boolean,
    isOnline: Boolean,
    previous: ProfileSyncStatus?,
): ProfileSyncStatus? = when (syncState) {
    SyncState.SYNCED -> null

    SyncState.PENDING -> ProfileSyncStatus.WAITING

    SyncState.SYNCING -> when {
        !isOnline -> previous.takeIf { it == ProfileSyncStatus.WAITING }

        showsSyncing(
            awaitsFullSync,
            wasSyncingOrFailed = previous == ProfileSyncStatus.FAILED || previous == ProfileSyncStatus.SYNCING,
        ) -> ProfileSyncStatus.SYNCING

        else -> previous.takeIf { it == ProfileSyncStatus.WAITING }
    }

    SyncState.FAILED -> ProfileSyncStatus.FAILED
}
