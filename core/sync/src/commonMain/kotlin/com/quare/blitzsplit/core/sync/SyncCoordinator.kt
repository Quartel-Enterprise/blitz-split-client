package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import software.quare.connectivity.NetworkConnectivityObserver

/*
 * Why: offline first. Room is what every screen observes; this reconciles it with Supabase
 * whenever the device is online and there is something local to send.
 */
class SyncCoordinator(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val scope: CoroutineScope,
) {
    private val syncLock = Mutex()
    private var started = false

    // Why: counts the syncs waiting on the lock too, so the state stays SYNCING between two queued ones.
    private val requestedSyncs = MutableStateFlow(0)
    private val lastSyncFailed = MutableStateFlow(false)

    fun observeState(): Flow<SyncState> = combine(
        requestedSyncs,
        lastSyncFailed,
        profileRepository.observeHasPendingChanges(),
    ) { requested, failed, hasPendingChanges ->
        when {
            requested > 0 -> SyncState.SYNCING
            failed -> SyncState.FAILED
            hasPendingChanges -> SyncState.PENDING
            else -> SyncState.SYNCED
        }
    }.distinctUntilChanged()

    fun start() {
        if (started) return
        started = true
        scope.launch { accountRepository.mirrorSession() }
        scope.launch { reviveSnoozedPrompt() }
        scope.launch { syncOnUserSwitch() }
        scope.launch { syncWhenDue() }
    }

    // Why: the retry outlives the screen that asked for it, like every other sync.
    fun retry() {
        scope.launch { syncUntilSettled() }
    }

    suspend fun syncNow(): Result<Unit> {
        requestedSyncs.update { it + 1 }
        return try {
            syncLock.withLock {
                suspendRunCatching {
                    profileRepository.observeProfile().first() ?: return@suspendRunCatching
                    val userId = accountRepository.ensureRegistered().getOrThrow()
                    profileRepository.downloadProfile(userId).getOrThrow()
                    profileRepository.uploadPendingProfile(userId).getOrThrow()
                    offerRecoveryCodeIfDue()
                }.also { result -> lastSyncFailed.value = result.isFailure }
            }
        } finally {
            requestedSyncs.update { it - 1 }
        }
    }

    /*
     * Why: a sync is due when the device can sync again (back online, or the first profile saved)
     * and whenever a change is queued while it already can. Changes going out don't trigger one.
     */
    private suspend fun syncWhenDue() {
        var couldSync = false
        combine(
            connectivityObserver.observe(),
            profileRepository.observeProfile(),
            profileRepository.observeHasPendingChanges(),
        ) { isOnline, profile, hasPendingChanges -> (isOnline && profile != null) to hasPendingChanges }
            .distinctUntilChanged()
            .collect { (canSync, hasPendingChanges) ->
                val isDue = canSync && (!couldSync || hasPendingChanges)
                couldSync = canSync
                if (isDue) syncUntilSettled()
            }
    }

    // Why: a change saved while a push was in flight is still pending after it, so it goes in another round.
    private suspend fun syncUntilSettled() {
        do {
            val result = syncNow()
        } while (result.isSuccess && profileRepository.observeHasPendingChanges().first())
    }

    private suspend fun offerRecoveryCodeIfDue() {
        val isAnonymous = accountRepository.observeStatus().first() is AccountStatus.Anonymous
        val prompt = accountRepository.observeRecoveryCodePrompt().first()
        if (isAnonymous && prompt == RecoveryCodePrompt.NOT_DUE) {
            accountRepository.createRecoveryCode()
        }
    }

    /*
     * Why: an identity that already belonged to another user switches the session to that user,
     * whose profile has to come down before this device's is pushed. A link keeps the same id.
     */
    private suspend fun syncOnUserSwitch() {
        var previousUserId: String? = null
        accountRepository
            .observeStatus()
            .map(::userIdOrNull)
            .distinctUntilChanged()
            .collect { userId ->
                val isSwitch = previousUserId != null && userId != null
                previousUserId = userId
                if (isSwitch) syncNow()
            }
    }

    private fun userIdOrNull(status: AccountStatus): String? = when (status) {
        AccountStatus.LocalOnly -> null
        is AccountStatus.Anonymous -> status.userId
        is AccountStatus.Connected -> status.userId
    }

    // Why: "Lembrar depois" shows the code again on the next launch, not right away.
    private suspend fun reviveSnoozedPrompt() {
        if (accountRepository.observeRecoveryCodePrompt().first() == RecoveryCodePrompt.SNOOZED) {
            accountRepository.updateRecoveryCodePrompt(RecoveryCodePrompt.DUE)
        }
    }
}
