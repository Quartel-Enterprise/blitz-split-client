package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.contacts.domain.repository.HiddenContactRepository
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.NotGroupCreatorException
import com.quare.blitzsplit.core.groups.domain.repository.GroupPinRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
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
    private val notificationPreferencesRepository: NotificationPreferencesRepository,
    private val groupRepository: GroupRepository,
    private val groupPinRepository: GroupPinRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val categoryRepository: CategoryRepository,
    private val activityRepository: ActivityRepository,
    private val hiddenContactRepository: HiddenContactRepository,
    private val firstDownload: FirstDownload,
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

    suspend fun syncNow(): Result<Unit> = runUnderSyncLock { syncLocked() }

    /*
     * Why: a request the server judges with every change made here ("Sair do grupo", "Apagar grupo"). Under the same
     * lock as the sync before it, so no other sync runs between the two or alongside the request: one whose download
     * read the server before it would bring back what the request just took off the device.
     */
    suspend fun <T> syncThen(request: suspend () -> Result<T>): Result<T> = runUnderSyncLock {
        syncLocked().fold(
            onSuccess = { suspendRunCatching { request().getOrThrow() } },
            onFailure = { Result.failure(it) },
        )
    }

    private suspend fun <T> runUnderSyncLock(block: suspend () -> Result<T>): Result<T> {
        requestedSyncs.update { it + 1 }
        return try {
            syncLock.withLock { block() }
        } finally {
            requestedSyncs.update { it - 1 }
        }
    }

    private suspend fun syncLocked(): Result<Unit> = suspendRunCatching {
        profileRepository.observeProfile().first() ?: return@suspendRunCatching
        // Why: the first sync with a profile restored here settles the first download, failed or not.
        try {
            syncSteps()
        } finally {
            firstDownload.end()
        }
    }.also { result -> lastSyncFailed.value = result.isFailure }

    private suspend fun syncSteps() {
        val userId = accountRepository.ensureRegistered().getOrThrow()
        profileRepository.downloadProfile(userId).getOrThrow()
        profileRepository.uploadPendingProfile(userId).getOrThrow()
        /*
         * Why: after the profile, whose download decides whose copy this device holds. A key, a
         * preference or a group that fails still fails the sync, but not before the steps that only
         * need the profile, nor each other.
         */
        val keysSynced = suspendRunCatching {
            paymentKeyRepository.downloadKeys(userId).getOrThrow()
            paymentKeyRepository.uploadPendingKeys(userId).getOrThrow()
        }
        val preferencesSynced = suspendRunCatching {
            notificationPreferencesRepository.downloadPreferences(userId).getOrThrow()
            notificationPreferencesRepository.uploadPendingPreferences(userId).getOrThrow()
        }
        /*
         * Why: the bills and the payments after their groups, which they need on the device to come
         * down and on the server to go up.
         */
        var areGroupsSynced = false
        val groupsSynced = suspendRunCatching {
            groupRepository.downloadGroups(userId).getOrThrow()
            groupRepository.uploadPendingGroups(userId).getOrThrow()
            areGroupsSynced = true
            expenseRepository.downloadExpenses(userId).getOrThrow()
            expenseRepository.uploadPendingExpenses(userId).getOrThrow()
            settlementRepository.downloadSettlements(userId).getOrThrow()
            settlementRepository.uploadPendingSettlements(userId).getOrThrow()
            categoryRepository.download(userId).getOrThrow()
            categoryRepository.uploadPending(userId).getOrThrow()
        }
        // Why: the pins need their groups too, but only order Início, so one that fails never holds the money back.
        val pinsSynced = if (areGroupsSynced) {
            suspendRunCatching {
                groupPinRepository.downloadPins(userId).getOrThrow()
                groupPinRepository.uploadPendingPins(userId).getOrThrow()
            }
        } else {
            Result.success(Unit)
        }
        /*
         * Why: after every push, whose events the server records as they arrive, so the feed comes down with them; up
         * first, so a charge sent here comes back with its place in the feed. A failure fails the sync like the others.
         */
        val activitySynced = suspendRunCatching {
            activityRepository.uploadPendingActivity(userId).getOrThrow()
            activityRepository.downloadActivity(userId).getOrThrow()
        }
        // Why: Contatos' hidden list is the user's own, like the keys, so it syncs apart from the groups.
        val hiddenContactsSynced = suspendRunCatching {
            hiddenContactRepository.download(userId).getOrThrow()
            hiddenContactRepository.uploadPending(userId).getOrThrow()
        }
        offerRecoveryCodeIfDue()
        signOutIfScheduledAndSynced()
        keysSynced.getOrThrow()
        preferencesSynced.getOrThrow()
        groupsSynced.getOrThrow()
        pinsSynced.getOrThrow()
        activitySynced.getOrThrow()
        hiddenContactsSynced.getOrThrow()
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
     * failure there leaves both for another try. Once it is gone the deletion succeeded, whatever happens here:
     * each step of erasing the device's copy runs on its own, so one that fails can't keep the others.
     */
    suspend fun deleteAccount(): Result<Unit> = scope
        .async {
            syncLock.withLock {
                accountRepository.deleteAccount().onSuccess {
                    // Why: the keys first, so a failure can't leave someone's CPF or wallet on a device with no profile.
                    suspendRunCatching { paymentKeyRepository.clear() }
                    suspendRunCatching { notificationPreferencesRepository.clear() }
                    suspendRunCatching { groupRepository.clear() }
                    suspendRunCatching { activityRepository.clear() }
                    suspendRunCatching { hiddenContactRepository.clear() }
                    suspendRunCatching { profileRepository.clear() }
                    suspendRunCatching { accountRepository.endDeletedSession() }
                }
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
     * Why: "Trocar link". Under the sync lock, so a download that read the group before can't write the old code
     * back over the new one, and in the app's scope, so leaving Convidar mid-call can't lose the new code. A failure
     * may hide a change the server made anyway (an answer lost on the way), so a sync brings the code it holds, also
     * once Convidar is gone; a refusal changed nothing, so it needs none.
     */
    suspend fun rotateInviteCode(groupId: String): Result<Unit> = scope
        .async {
            syncLock
                .withLock { groupRepository.rotateInviteCode(groupId) }
                .onFailure { if (it !is NotGroupCreatorException) retry() }
        }.await()

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
     * on the profile, the keys, the preferences, the groups, the pending bills' and payments' versions, the hidden
     * contacts and the pins makes each new change count, while one going out triggers nothing.
     */
    private suspend fun syncWhenDue() {
        var couldSync = false
        val lists = combine(
            paymentKeyRepository.observeKeys(),
            notificationPreferencesRepository.observePreferences(),
            groupRepository.observeGroups(),
            observePendingMoneyVersions(),
            hiddenContactRepository.observeHidden(),
            groupPinRepository.observePinnedGroupIds(),
        ) { lists -> lists.toList() }
        combine(
            connectivityObserver.observe(),
            profileRepository.observeProfile(),
            lists,
            observeHasPendingChanges(),
        ) { isOnline, profile, lists, hasPendingChanges ->
            Triple(isOnline && profile != null, profile to lists, hasPendingChanges)
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
        notificationPreferencesRepository.clear()
        groupRepository.clear()
        activityRepository.clear()
        hiddenContactRepository.clear()
        profileRepository.clear()
    }

    private fun observeHasPendingChanges(): Flow<Boolean> = combine(
        profileRepository.observeHasPendingChanges(),
        paymentKeyRepository.observeHasPendingChanges(),
        notificationPreferencesRepository.observeHasPendingChanges(),
        groupRepository.observeHasPendingChanges(),
        combine(
            expenseRepository.observeHasPendingChanges(),
            settlementRepository.observeHasPendingChanges(),
            categoryRepository.observeHasPendingChanges(),
            groupPinRepository.observeHasPendingChanges(),
            hiddenContactRepository.observeHasPendingChanges(),
        ) { hasPendingExpenses, hasPendingSettlements, hasPendingCategories, hasPendingPins, hasPendingHidden ->
            hasPendingExpenses || hasPendingSettlements || hasPendingCategories || hasPendingPins || hasPendingHidden
        },
    ) { hasPendingProfile, hasPendingKeys, hasPendingPreferences, hasPendingGroups, hasPendingMoney ->
        hasPendingProfile || hasPendingKeys || hasPendingPreferences || hasPendingGroups || hasPendingMoney
    }.distinctUntilChanged()

    /*
     * Why: the versions of the bills, the payments and the group categories waiting to go up, which change with each
     * one saved. Kept apart, so a bill going up while a payment of the same version is saved still counts as a change.
     */
    private fun observePendingMoneyVersions(): Flow<List<List<Long>>> = combine(
        expenseRepository.observePendingVersions(),
        settlementRepository.observePendingVersions(),
        categoryRepository.observePendingVersions(),
    ) { expenses, settlements, categories -> listOf(expenses, settlements, categories) }

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
