package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.InactivityExpiry
import com.quare.blitzsplit.core.model.userIdOrNull
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.repository.AppInviteRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.domain.repository.PushDeviceRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Why: how long a failed call waits before each try again; after the last one it waits for the next change.
private val pushRetryDelays = listOf(30.seconds, 2.minutes, 10.minutes)
private const val EXPIRED_NOTICE_ATTEMPTS = 3

/*
 * Why: Atividade's push notifications go to the devices of the account signed in, so this device's token goes to
 * the server for each user it signs in as, an anonymous one included, once online. With no account signed in, or
 * with Notifications' "Push notifications" switch off on this device, the token is removed instead, also once
 * online: a sign out made offline, or one whose call failed, would otherwise leave the device getting the old
 * account's notices, and the server forgets a token whoever holds it. A failure is tried again a few times, further
 * apart each time, since a switch turned off would otherwise leave pushes coming until something else changed; after
 * that it waits for the next change: back online, another token, another user or the switch, which also cancels the
 * retries of the one before.
 */
internal suspend fun registerPushDevice(
    pushDeviceRepository: PushDeviceRepository,
    accountRepository: AccountRepository,
    connectivityObserver: NetworkConnectivityObserver,
) {
    combine(
        pushDeviceRepository.observeDevice(),
        accountRepository.observeStatus().map { it.userIdOrNull() },
        pushDeviceRepository.observeIsEnabled(),
        connectivityObserver.observe(),
    ) { device, userId, isEnabled, isOnline ->
        if (device != null && isOnline) PushRegistration(device, userId.takeIf { isEnabled }) else null
    }.distinctUntilChanged()
        .filterNotNull()
        .collectLatest { registration ->
            val send: suspend () -> Result<Unit> = {
                val userId = registration.userId
                if (userId == null) pushDeviceRepository.unregister() else pushDeviceRepository.register(userId)
            }
            for (wait in pushRetryDelays) {
                if (send().isSuccess) return@collectLatest
                delay(wait)
            }
            send()
        }
}

// Why: the token and whom it goes to, null to take it off the server.
private data class PushRegistration(
    val device: PushDevice,
    val userId: String?,
)

/*
 * Why: a dismissal of the invite ("Get the app") goes up with a sync of its own once online, apart from the pending
 * changes, so one that can't go up never holds a sync, the sync state or a sign out back; the next sync tries again.
 */
internal suspend fun syncAppInviteDismissals(
    appInviteRepository: AppInviteRepository,
    profileRepository: ProfileRepository,
    connectivityObserver: NetworkConnectivityObserver,
    sync: suspend () -> Result<Unit>,
) {
    combine(
        appInviteRepository.observeHasPendingChanges(),
        connectivityObserver.observe(),
        profileRepository.observeProfile(),
    ) { isPending, isOnline, profile -> isPending && isOnline && profile != null }
        .distinctUntilChanged()
        .filter { it }
        .collect { sync() }
}

/*
 * Why: a sync while the session waited for its refresh was put off and left the device as it was, and nothing else
 * changes when its token comes back (the same user, maybe online all along), so that sync runs again then. One that
 * went through needs no other.
 */
internal suspend fun syncOnSessionRecovery(
    accountRepository: AccountRepository,
    lastSyncWentThrough: () -> Boolean,
    sync: suspend () -> Unit,
) {
    accountRepository.observeSessionRecoveries().collect { if (!lastSyncWentThrough()) sync() }
}

/*
 * Why: the session of an account without Google or Apple ended on its own, which nothing else answers with a sync:
 * the device is as online as before and nothing is queued. One runs now, since registering is what asks the server
 * whether it deleted the account, and what signs in a new user when it didn't.
 */
internal suspend fun syncOnLostAnonymousUser(
    accountRepository: AccountRepository,
    sync: suspend () -> Unit,
) {
    accountRepository
        .observeAccount()
        .map { account -> account.hasLostAnonymousUser }
        .distinctUntilChanged()
        .filter { hasLost -> hasLost }
        .collect { sync() }
}

/*
 * Why: the server deleted the account for inactivity, so its copy leaves the device as in a deletion asked here, and
 * the welcome tells why. Under the sync lock, and read again there: a sign out that got ahead already erased it. A
 * profile that failed to go keeps the account pending, so no sync registers a new user with it and the next launch
 * erases again.
 */
internal suspend fun eraseExpiredAccount(
    accountRepository: AccountRepository,
    profileRepository: ProfileRepository,
    syncLock: Mutex,
    eraseDeviceCopy: suspend () -> Unit,
) {
    accountRepository
        .observeAccount()
        .map { account -> account.inactivityExpiry == InactivityExpiry.PENDING }
        .distinctUntilChanged()
        .filter { isPending -> isPending }
        .collect {
            syncLock.withLock {
                val account = accountRepository.observeAccount().first()
                if (account.inactivityExpiry == InactivityExpiry.PENDING) {
                    eraseDeviceCopy()
                    val isErased = suspendRunCatching { profileRepository.observeProfile().first() == null }
                    if (isErased.getOrDefault(false)) leaveExpiredNotice(accountRepository)
                }
            }
        }
}

/*
 * Why: with the copy erased, an account left pending would refuse every sync of a profile made on the welcome and
 * erase it at the next launch, so a write that fails is tried again before it is given up.
 */
private suspend fun leaveExpiredNotice(accountRepository: AccountRepository) {
    repeat(EXPIRED_NOTICE_ATTEMPTS) {
        if (suspendRunCatching { accountRepository.endExpiredSession() }.isSuccess) return
    }
}
