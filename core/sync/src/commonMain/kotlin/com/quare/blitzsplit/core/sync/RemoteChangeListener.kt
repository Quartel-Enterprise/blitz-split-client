package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.profile.domain.repository.ProfileRepository
import com.quare.blitzsplit.core.sync.domain.model.RemoteChange
import com.quare.blitzsplit.core.sync.domain.repository.RemoteChangeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import software.quare.connectivity.NetworkConnectivityObserver

/*
 * Why: real time (beta feedback 13): another device's change reaches this one while the app is open, without waiting
 * for the next sync. Only in front, as Bible Planner learned: a token refreshed in the background reaches a socket that
 * died, which supabase-kt's setAuth crashes on. And online with an account and a profile, as any sync. Leaving any of
 * those leaves the channel; coming back joins it again, which SyncCoordinator catches up on.
 */
class RemoteChangeListener(
    private val remoteChangeRepository: RemoteChangeRepository,
    private val appForeground: AppForeground,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
) {
    // Why: onJoined runs the moment the channel joins, before anything heard after it.
    suspend fun listen(
        onJoined: () -> Unit,
        onChanges: suspend (userId: String, changes: List<RemoteChange>) -> Unit,
    ) {
        combine(
            appForeground.observe(),
            connectivityObserver.observe(),
            accountRepository.observeStatus().map(::userIdOrNull),
            profileRepository.observeProfile().map { profile -> profile != null },
        ) { isForeground, isOnline, userId, hasProfile ->
            userId?.takeIf { isForeground && isOnline && hasProfile }
        }.distinctUntilChanged()
            .collectLatest { userId -> if (userId != null) listen(userId, onJoined, onChanges) }
    }

    /*
     * Why: the changes come in bursts (a sync pushing a queue, a guest merged), so what is heard within a moment goes
     * as one batch. A channel that fails listens again a little later, since real time is only a nicety over the sync.
     */
    private suspend fun listen(
        userId: String,
        onJoined: () -> Unit,
        onChanges: suspend (userId: String, changes: List<RemoteChange>) -> Unit,
    ) {
        coroutineScope {
            val heard = Channel<RemoteChange>(Channel.UNLIMITED)
            launch {
                remoteChangeRepository
                    .observeChanges()
                    .retryWhen { _, _ ->
                        delay(RELISTEN_MILLIS)
                        true
                    }.collect { change -> if (change is RemoteChange.Joined) onJoined() else heard.send(change) }
            }
            while (true) {
                val batch = mutableListOf(heard.receive())
                delay(BATCH_MILLIS)
                while (true) batch += heard.tryReceive().getOrNull() ?: break
                onChanges(userId, batch)
            }
        }
    }

    private companion object {
        // Why: how long a change heard waits for the rest of its burst.
        const val BATCH_MILLIS = 500L

        // Why: how long a channel that failed (the server refused it, the socket gave up) waits to listen again.
        const val RELISTEN_MILLIS = 30_000L
    }
}
