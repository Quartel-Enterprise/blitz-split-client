package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.model.userIdOrNull
import com.quare.blitzsplit.core.profile.domain.model.PushDevice
import com.quare.blitzsplit.core.profile.domain.repository.AppInviteRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.profile.domain.repository.PushDeviceRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import software.quare.connectivity.NetworkConnectivityObserver

/*
 * Why: Atividade's push notifications go to the devices of the account signed in, so this device's token goes to
 * the server for each user it signs in as, an anonymous one included, once online. With no account signed in, or
 * with Notifications' "Push notifications" switch off on this device, the token is removed instead, also once
 * online: a sign out made offline, or one whose call failed, would otherwise leave the device getting the old
 * account's notices, and the server forgets a token whoever holds it. A failure waits for the next change: back
 * online, another token, another user or the switch.
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
        .collect { registration ->
            val userId = registration.userId
            if (userId == null) pushDeviceRepository.unregister() else pushDeviceRepository.register(userId)
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
