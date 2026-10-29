package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
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
    private val paymentKeyRepository: PaymentKeyRepository,
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
        observeHasPendingChanges(),
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
        scope.launch { accountRepository.keepAppleTokens() }
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
                    /*
                     * Why: after the profile, whose download decides whose copy this device holds. A key that
                     * fails still fails the sync, but not before the steps that only need the profile.
                     */
                    val keysSynced = suspendRunCatching {
                        paymentKeyRepository.downloadKeys(userId).getOrThrow()
                        paymentKeyRepository.uploadPendingKeys(userId).getOrThrow()
                    }
                    offerRecoveryCodeIfDue()
                    signOutIfScheduledAndSynced()
                    keysSynced.getOrThrow()
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
     * Why: under the sync lock and in the app's scope, like the sign out. The server's copy goes first, and a
     * failure there leaves both for another try. Once it is gone there is nothing left to retry, so the device's
     * copy is erased step by step: a step that fails doesn't keep the next from running.
     */
    suspend fun deleteAccount(): Result<Unit> = scope
        .async {
            syncLock.withLock {
                suspendRunCatching { accountRepository.deleteAccount().getOrThrow() }
                    .onSuccess { eraseDeletedAccount() }
            }
        }.await()

    /*
     * Why: in the app's scope, so leaving the screen once the server has unlinked can't lose the new code, and
     * under the sync lock, so a sync that read the user before can't mirror the Google or Apple account back.
     */
    suspend fun removeSocialIdentity(): Result<Unit> = scope
        .async { syncLock.withLock { accountRepository.removeSocialIdentity() } }
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
     * on the profile and the keys themselves makes each new change count, while one going out triggers nothing.
     */
    private suspend fun syncWhenDue() {
        var couldSync = false
        combine(
            connectivityObserver.observe(),
            profileRepository.observeProfile(),
            paymentKeyRepository.observeKeys(),
            observeHasPendingChanges(),
        ) { isOnline, profile, keys, hasPendingChanges ->
            Triple(isOnline && profile != null, profile to keys, hasPendingChanges)
        }.distinctUntilChanged()
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
        } while (result.isSuccess && observeHasPendingChanges().first())
    }

    /*
     * Why: only a Google or Apple account schedules it, and the profile comes back only with that account.
     * The schedule is read last, right before the session ends, so a cancel up to then still keeps it.
     */
    private suspend fun signOutIfScheduledAndSynced() {
        if (observeHasPendingChanges().first()) return
        val account = accountRepository.observeAccount().first()
        if (account.signOutWhenSynced && account.status is AccountStatus.Connected) clearSession()
    }

    /*
     * Why: the account goes first. If the profile then failed to clear, the person stays on the profile,
     * now without an account, and can erase again; the other way round would leave a live Google or
     * Apple session behind the welcome, where a new profile would overwrite the account's.
     */
    private suspend fun clearSession() {
        accountRepository.signOut()
        // Why: the keys first, so a failure can't leave someone's CPF or wallet on a device with no profile.
        paymentKeyRepository.clear()
        profileRepository.clear()
    }

    // Why: the keys first, as in clearSession, so a failure can't leave someone's CPF or wallet behind a profile.
    private suspend fun eraseDeletedAccount() {
        suspendRunCatching { accountRepository.signOut() }
        suspendRunCatching { paymentKeyRepository.clear() }
        suspendRunCatching { profileRepository.clear() }
    }

    private fun observeHasPendingChanges(): Flow<Boolean> = combine(
        profileRepository.observeHasPendingChanges(),
        paymentKeyRepository.observeHasPendingChanges(),
    ) { hasPendingProfile, hasPendingKeys -> hasPendingProfile || hasPendingKeys }.distinctUntilChanged()

    private suspend fun offerRecoveryCodeIfDue() {
        val account = accountRepository.observeAccount().first()
        if (account.status is AccountStatus.Anonymous && account.recoveryCodePrompt == RecoveryCodePrompt.NOT_DUE) {
            accountRepository.createRecoveryCode()
        }
    }

    /*
     * Why: an identity that already belonged to another user switches the session to that user, whose profile
     * replaces this device's copy (see ProfileRepository.downloadProfile), so it syncs right away. A link keeps
     * the same id.
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
