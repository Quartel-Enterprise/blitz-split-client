package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.activity.domain.repository.ActivityRepository
import com.quare.blitzsplit.core.contacts.domain.repository.HiddenContactRepository
import com.quare.blitzsplit.core.expenses.domain.repository.BillAttachmentRepository
import com.quare.blitzsplit.core.expenses.domain.repository.BillCommentRepository
import com.quare.blitzsplit.core.expenses.domain.repository.CategoryRepository
import com.quare.blitzsplit.core.expenses.domain.repository.ExpenseRepository
import com.quare.blitzsplit.core.expenses.domain.repository.SettlementRepository
import com.quare.blitzsplit.core.groups.domain.model.NotGroupCreatorException
import com.quare.blitzsplit.core.groups.domain.repository.GroupIncomeRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupPinRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.model.AccountStatus
import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import com.quare.blitzsplit.core.model.SyncState
import com.quare.blitzsplit.core.model.userIdOrNull
import com.quare.blitzsplit.core.profile.domain.repository.AppInviteRepository
import com.quare.blitzsplit.core.profile.domain.repository.NotificationPreferencesRepository
import com.quare.blitzsplit.core.profile.domain.repository.PaymentKeyRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.domain.repository.PushDeviceRepository
import com.quare.blitzsplit.core.provider.crashlytics.CrashReporter
import com.quare.blitzsplit.core.sync.domain.model.RemoteChange
import com.quare.blitzsplit.core.sync.domain.model.RemotePull
import com.quare.blitzsplit.core.sync.domain.repository.RemoteChangeRepository
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
    private val appInviteRepository: AppInviteRepository,
    private val groupRepository: GroupRepository,
    private val groupPinRepository: GroupPinRepository,
    private val groupIncomeRepository: GroupIncomeRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val categoryRepository: CategoryRepository,
    private val personalCategoryData: PersonalCategoryData,
    private val billCommentRepository: BillCommentRepository,
    private val billAttachmentRepository: BillAttachmentRepository,
    private val activityRepository: ActivityRepository,
    private val hiddenContactRepository: HiddenContactRepository,
    private val pushDeviceRepository: PushDeviceRepository,
    private val firstDownload: FirstDownload,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val scope: CoroutineScope,
    crashReporter: CrashReporter,
    remoteChangeRepository: RemoteChangeRepository,
    appForeground: AppForeground,
) {
    private val syncLock = Mutex()
    private val remoteChangeListener = RemoteChangeListener(
        remoteChangeRepository = remoteChangeRepository,
        appForeground = appForeground,
        connectivityObserver = connectivityObserver,
        accountRepository = accountRepository,
        profileRepository = profileRepository,
    )
    private val remotePuller = RemotePuller(
        profileRepository = profileRepository,
        paymentKeyRepository = paymentKeyRepository,
        groupRepository = groupRepository,
        groupIncomeRepository = groupIncomeRepository,
        categoryRepository = categoryRepository,
        expenseRepository = expenseRepository,
        settlementRepository = settlementRepository,
        billCommentRepository = billCommentRepository,
        billAttachmentRepository = billAttachmentRepository,
        activityRepository = activityRepository,
    )
    private var started = false

    // Why: counts the syncs waiting on the lock too, so the state stays SYNCING between two queued ones.
    private val requestedSyncs = MutableStateFlow(0)
    private val outcomes = SyncOutcomes(firstDownload, crashReporter, connectivityObserver)

    /*
     * Why: how many syncs began reading the groups, the bills and the rest real time tells of, so a channel that joined
     * can tell whether one read them after it: one already running at the join (the launch's, the one coming back online
     * starts) usually reaches them after the join completes.
     */
    private val groupReadsStarted = MutableStateFlow(0L)

    // Why: offline, a failed sync is expected and only a change still waiting on the device is worth telling.
    fun observeState(): Flow<SyncState> = combine(
        requestedSyncs,
        outcomes.lastSyncFailed,
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
        scope.launch { syncOnSessionRecovery(accountRepository, outcomes.lastSyncFailed, ::syncUntilSettled) }
        scope.launch { syncWhenDue() }
        scope.launch { registerPushDevice(pushDeviceRepository, accountRepository, connectivityObserver) }
        scope.launch {
            syncAppInviteDismissals(
                appInviteRepository,
                profileRepository,
                connectivityObserver,
                ::syncNow,
            )
        }
        scope.launch { endFirstDownloadOffline() }
        scope.launch { followRemoteChanges() }
    }

    // Why: the retry outlives the screen that asked for it, like every other sync.
    fun retry() {
        scope.launch { syncUntilSettled() }
    }

    suspend fun syncNow(): Result<Unit> = runUnderSyncLock { syncLocked() }

    /*
     * Why: a request the server judges with every change made here ("Sair do grupo", "Apagar grupo"). Under the same
     * lock as the sync before it, so no other sync runs between the two or alongside the request: one whose download
     * read the server before it would bring back what the request just took off the device. With nothing waiting to
     * go up the server already has every change made here, so the request goes at once: a whole sync first, a round
     * trip per part, is what made deleting a group slow.
     */
    suspend fun <T> syncThen(request: suspend () -> Result<T>): Result<T> = runUnderSyncLock {
        val synced = suspendRunCatching {
            if (observeHasPendingChanges().first()) syncLocked().getOrThrow()
        }
        synced.fold(
            onSuccess = { suspendRunCatching { request().getOrThrow() } },
            onFailure = { Result.failure(it) },
        )
    }

    // Why: work a screen hands over and doesn't wait for ("Apagar grupo"), which outlives it like every sync.
    fun runInBackground(block: suspend () -> Unit) {
        scope.launch { block() }
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
        val userId = try {
            syncSteps()
        } finally {
            firstDownload.end()
        }
        /*
         * Why: only a sync that brought every part down completes the account's copy; one that failed leaves it awaited,
         * and so does a switch to another user while it ran, whose copy this sync never brought.
         */
        val account = accountRepository.observeAccount().first()
        if (account.awaitsFullSync && account.status.userIdOrNull() == userId) {
            accountRepository.updateAwaitsFullSync(false)
        }
    }.also { result ->
        outcomes.onSyncEnded(result)
    }

    // Why: returns the user it synced, whose copy it completed.
    private suspend fun syncSteps(): String {
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
         * Why: "Get the app" (page 14): this platform recorded, and the invite's dismissal and the account's phone use.
         * Only a nicety, which every platform runs but only the web and the Mac show, so a failure never fails the sync;
         * the next one tries again.
         */
        appInviteRepository.sync(userId)
        // Why: the person's category map (flow 9) and custom colors, theirs alone like the keys.
        val termsSynced = personalCategoryData.sync(userId)
        /*
         * Why: the bills and the payments after their groups, which they need on the device to come
         * down and on the server to go up, and after the group's categories, so a bill never reaches
         * another device before the category it is in.
         */
        var areGroupsSynced = false
        var incomesSynced = Result.success(Unit)
        groupReadsStarted.update { it + 1 }
        val groupsSynced = suspendRunCatching {
            if (firstDownload.isPending) {
                // Why: the count is only a nicety, so a pass that fails ends it and the full download below goes on.
                suspendRunCatching { downloadGroupByGroup(userId) }.onFailure { firstDownload.end() }
            }
            groupRepository.downloadGroups(userId).getOrThrow()
            groupRepository.uploadPendingGroups(userId).getOrThrow()
            areGroupsSynced = true
            // Why: right after their group, which real time tells the members of, but never holding the money back.
            incomesSynced = groupIncomeRepository.sync(userId)
            categoryRepository.download(userId).getOrThrow()
            categoryRepository.uploadPending(userId).getOrThrow()
            expenseRepository.downloadExpenses(userId).getOrThrow()
            expenseRepository.uploadPendingExpenses(userId).getOrThrow()
            settlementRepository.downloadSettlements(userId).getOrThrow()
            settlementRepository.uploadPendingSettlements(userId).getOrThrow()
        }
        /*
         * Why: "Pagar Ana" (4.5) shows the keys of the people the user shares a group with, which the server decides
         * by the groups it holds, so after they went up. All at once, so a key deleted or someone gone leaves too.
         */
        val memberKeysSynced = suspendRunCatching {
            paymentKeyRepository.downloadMemberKeys(userId).getOrThrow()
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
        // Why: the bills' comments after the bills they are on, which the server needs to take them; never the money's.
        val commentsSynced = if (groupsSynced.isSuccess) {
            suspendRunCatching {
                billCommentRepository.download(userId).getOrThrow()
                billCommentRepository.uploadPending(userId).getOrThrow()
            }
        } else {
            Result.success(Unit)
        }
        // Why: the photos as the comments, after their bills; the images go up with their rows.
        val attachmentsSynced = if (groupsSynced.isSuccess) {
            suspendRunCatching {
                billAttachmentRepository.download(userId).getOrThrow()
                billAttachmentRepository.uploadPending(userId).getOrThrow()
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
        termsSynced.getOrThrow()
        groupsSynced.getOrThrow()
        memberKeysSynced.getOrThrow()
        pinsSynced.getOrThrow()
        incomesSynced.getOrThrow()
        commentsSynced.getOrThrow()
        attachmentsSynced.getOrThrow()
        activitySynced.getOrThrow()
        hiddenContactsSynced.getOrThrow()
        return userId
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
                    suspendRunCatching { appInviteRepository.clear() }
                    suspendRunCatching { personalCategoryData.clear() }
                    suspendRunCatching { groupRepository.clear() }
                    suspendRunCatching { activityRepository.clear() }
                    suspendRunCatching { hiddenContactRepository.clear() }
                    suspendRunCatching { profileRepository.clear() }
                    suspendRunCatching { accountRepository.endDeletedSession() }
                }
            }
        }.await()

    /*
     * Why: "Trocar link", under the sync lock so a download that read the group before can't write the old code back,
     * in the app's scope so leaving Convidar keeps it. A failure may hide a change the server made anyway, so a sync
     * brings the code it holds; a refusal changed nothing, so it needs none.
     */
    suspend fun rotateInviteCode(groupId: String): Result<Unit> = scope
        .async {
            runUnderSyncLock {
                // Why: a group created here goes up first; a sync failing further on (a refused bill) still took it up.
                val synced = if (groupRepository.isOnServer(groupId)) Result.success(Unit) else syncLocked()
                val canRotate = synced.isSuccess || groupRepository.isOnServer(groupId)
                if (canRotate) groupRepository.rotateInviteCode(groupId) else synced
            }.onFailure { if (it !is NotGroupCreatorException) retry() }
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
     * contacts, the pins, the category map's terms, the comments and the attachments makes each new change count, while one going out triggers
     * nothing.
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
            personalCategoryData.observeChanges(),
            billCommentRepository.observePendingIds(),
            billAttachmentRepository.observePendingVersions(),
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
        appInviteRepository.clear()
        personalCategoryData.clear()
        groupRepository.clear()
        activityRepository.clear()
        hiddenContactRepository.clear()
        profileRepository.clear()
    }

    // Why: anything waiting on the device to go up, whichever part it belongs to.
    private fun observeHasPendingChanges(): Flow<Boolean> = observeAnyPending(
        profileRepository.observeHasPendingChanges(),
        paymentKeyRepository.observeHasPendingChanges(),
        notificationPreferencesRepository.observeHasPendingChanges(),
        personalCategoryData.observeHasPendingChanges(),
        billCommentRepository.observeHasPendingChanges(),
        billAttachmentRepository.observeHasPendingChanges(),
        groupRepository.observeHasPendingChanges(),
        expenseRepository.observeHasPendingChanges(),
        settlementRepository.observeHasPendingChanges(),
        categoryRepository.observeHasPendingChanges(),
        groupPinRepository.observeHasPendingChanges(),
        groupIncomeRepository.observeHasPendingChanges(),
        hiddenContactRepository.observeHasPendingChanges(),
    )

    /*
     * Why: the versions of the bills, payments, group categories and incomes waiting to go up, which change with each
     * one saved. Kept apart, so a bill going up while a payment of the same version is saved still counts as a change.
     */
    private fun observePendingMoneyVersions(): Flow<List<List<Long>>> = combine(
        expenseRepository.observePendingVersions(),
        settlementRepository.observePendingVersions(),
        categoryRepository.observePendingVersions(),
        groupIncomeRepository.observePendingVersions(),
    ) { expenses, settlements, categories, incomes -> listOf(expenses, settlements, categories, incomes) }

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
            .map { it.userIdOrNull() }
            .distinctUntilChanged()
            .collect { userId ->
                val isSwitch = previousUserId != null && userId != null
                previousUserId = userId
                if (isSwitch) syncUntilSettled()
            }
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

    /*
     * Why: a first download (5.1d) asks the server which groups are coming, then brings each one whole, the latest
     * activity first, so Início and Grupos count "3 de 7 grupos" and what came is usable before the rest. A group's
     * rows are all read first, then written in Room's foreign key order with no round trip in between, so no screen
     * shows a group with its bills and without its payments (RemotePuller.downloadGroup). The count stays until the sync settles; the
     * full download right after it takes off what left meanwhile and brings the rest of the account.
     */
    private suspend fun downloadGroupByGroup(userId: String) {
        val groupIds = groupRepository.fetchFirstDownloadGroupIds().getOrThrow()
        // Why: with none coming there is nothing to count, so it never reads "0 de 0 grupos".
        if (groupIds.isNotEmpty()) firstDownload.expect(groupIds.size)
        groupIds.forEach { groupId ->
            remotePuller.downloadGroup(userId, groupId)
            firstDownload.groupArrived()
        }
        // Why: "7 de 7 grupos" stays until the rest of the sync settles, which ends it, never a bar that runs again.
    }

    /*
     * Why: a join's catch-up and a batch's pull run in the app's scope like any sync, so a slow one never holds the
     * channel's buffer nor stops halfway when the app leaves the front; the pull checks under the lock that its user is
     * still the one signed in.
     */
    private suspend fun followRemoteChanges() {
        remoteChangeListener.listen(
            onJoined = {
                val readsBeforeJoin = groupReadsStarted.value
                scope.launch { catchUp(readsBeforeJoin) }
            },
            onChanges = { userId, changes -> scope.launch { pullRemoteChanges(userId, changes) } },
        )
    }

    /*
     * Why: what happened while nobody listened (in the background, offline, the socket down) comes with a full sync,
     * unless one read the groups since the channel joined, which already brought everything after it.
     */
    private suspend fun catchUp(readsBeforeJoin: Long) {
        // Why: checked again under the lock, but first without it, so a catch-up with nothing to do never shows as a sync.
        if (groupReadsStarted.value > readsBeforeJoin) return
        val result = runUnderSyncLock {
            if (groupReadsStarted.value > readsBeforeJoin) Result.success(Unit) else syncLocked()
        }
        if (result.isSuccess && observeHasPendingChanges().first()) syncUntilSettled()
    }

    /*
     * Why: under the sync lock, like any download, so a sync's writes and the pull's never interleave. It doesn't count
     * as a sync for the badge, which only asks for attention. What only the full sync drops, and a pull that failed, go to
     * the full sync, which tries again and reports it.
     */
    private suspend fun pullRemoteChanges(
        userId: String,
        changes: List<RemoteChange>,
    ) {
        val isPulled = syncLock.withLock {
            suspendRunCatching {
                // Why: a sign out, an erased profile or another user may have taken the lock first.
                if (!isSignedInWithProfile(userId)) return@suspendRunCatching true
                val pull = RemotePull.of(
                    changes = changes,
                    userId = userId,
                )
                if (!pull.isFullSync) remotePuller.pull(userId, pull)
                !pull.isFullSync
            }.getOrDefault(false)
        }
        if (!isPulled) scope.launch { syncUntilSettled() }
    }

    private suspend fun isSignedInWithProfile(userId: String): Boolean =
        accountRepository.observeStatus().first().userIdOrNull() == userId &&
            profileRepository.observeProfile().first() != null

    /*
     * Why: a first download ends with a sync, which never runs offline, so offline its count goes; the account still
     * awaits its full sync, which Início and Grupos show as waiting for the internet (see ObserveFirstDownload).
     */
    private suspend fun endFirstDownloadOffline() {
        combine(
            connectivityObserver.observe(),
            profileRepository.observeProfile(),
        ) { isOnline, profile -> !isOnline && profile != null }
            .distinctUntilChanged()
            .collect { isOfflineWithProfile -> if (isOfflineWithProfile) firstDownload.end() }
    }
}
