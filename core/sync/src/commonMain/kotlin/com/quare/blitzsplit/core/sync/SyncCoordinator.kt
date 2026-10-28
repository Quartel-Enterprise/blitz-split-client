package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
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

    // Why: offline, a failed sync is expected and only a change still waiting on the device is worth telling.
    fun observeState(): Flow<SyncState> = combine(
        requestedSyncs,
        lastSyncFailed,
        profileRepository.observeHasPendingChanges(),
        connectivityObserver.observe(),
    ) { requested, failed, hasPendingChanges, isOnline ->
        when {
            requested > 0 -> SyncState.SYNCING
            failed && isOnline -> SyncState.FAILED
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
                    signOutIfScheduledAndSynced()
                }.also { result -> lastSyncFailed.value = result.isFailure }
            }
        } finally {
            requestedSyncs.update { it - 1 }
        }
    }

    /*
     * Why: under the sync lock, so a push in flight ends before the copy it reads is gone. In the app's
     * scope, so leaving the screen midway can't end the account and keep the profile.
     */
    suspend fun signOut(): Result<Unit> = scope
        .async { syncLock.withLock { suspendRunCatching { clearSession() } } }
        .await()

    /*
     * Why: "Sair ao sincronizar" keeps the queued changes on the device until they are up, so the
     * session ends after the sync that empties the queue, now if nothing waits, or once back online.
     */
    fun signOutWhenSynced() {
        scope.launch {
            accountRepository.updateSignOutWhenSynced(isScheduled = true)
            syncUntilSettled()
        }
    }

    suspend fun cancelSignOutWhenSynced() {
        accountRepository.updateSignOutWhenSynced(isScheduled = false)
    }

    /*
     * Why: a sync is due when the device can sync again (back online, or the first profile saved)
     * and whenever a change is queued while it already can, also after a push that failed. Keying
     * on the profile itself makes each new change count, while one going out triggers nothing.
     */
    private suspend fun syncWhenDue() {
        var couldSync = false
        combine(
            connectivityObserver.observe(),
            profileRepository.observeProfile(),
            profileRepository.observeHasPendingChanges(),
        ) { isOnline, profile, hasPendingChanges -> Triple(isOnline && profile != null, profile, hasPendingChanges) }
            .distinctUntilChanged()
            .collect { (canSync, _, hasPendingChanges) ->
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

    // Why: only a Google or Apple account schedules it, and the profile comes back only with that account.
    private suspend fun signOutIfScheduledAndSynced() {
        val account = accountRepository.observeAccount().first()
        val isScheduled = account.signOutWhenSynced && account.status is AccountStatus.Connected
        if (isScheduled && !profileRepository.observeHasPendingChanges().first()) clearSession()
    }

    /*
     * Why: the account goes first. If the profile then failed to clear, the person stays on the profile,
     * now without an account, and can erase again; the other way round would leave a live Google or
     * Apple session behind the welcome, where a new profile would overwrite the account's.
     */
    private suspend fun clearSession() {
        accountRepository.signOut()
        profileRepository.clear()
    }

    private suspend fun offerRecoveryCodeIfDue() {
        val account = accountRepository.observeAccount().first()
        if (account.status is AccountStatus.Anonymous && account.recoveryCodePrompt == RecoveryCodePrompt.NOT_DUE) {
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
                if (isSwitch) syncUntilSettled()
            }
    }

    private fun userIdOrNull(status: AccountStatus): String? = when (status) {
        AccountStatus.LocalOnly -> null
        is AccountStatus.Anonymous -> status.userId
        is AccountStatus.Connected -> status.userId
    }

    /*
     * Why: "Lembrar depois" shows the code again on the next launch, not right away. A saved code the
     * app closed on while it was opened from the profile is still saved.
     */
    private suspend fun reviveSnoozedPrompt() {
        when (accountRepository.observeAccount().first().recoveryCodePrompt) {
            RecoveryCodePrompt.SNOOZED -> accountRepository.updateRecoveryCodePrompt(RecoveryCodePrompt.DUE)
            RecoveryCodePrompt.REVIEW -> accountRepository.updateRecoveryCodePrompt(RecoveryCodePrompt.SAVED)
            else -> Unit
        }
    }
}
