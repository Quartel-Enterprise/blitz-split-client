package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.account.domain.repository.AccountRepository
import com.quare.blitzsplit.core.sync.FirstDownload
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import software.quare.connectivity.NetworkConnectivityObserver

class ObserveFirstDownloadUseCase(
    private val firstDownload: FirstDownload,
    private val accountRepository: AccountRepository,
    private val connectivityObserver: NetworkConnectivityObserver,
) : ObserveFirstDownload {
    /*
     * Why: the count while this launch downloads group by group; otherwise, while the account still awaits its first
     * full sync (a relaunch midway, a pass that failed, a switch to another user), a download whose count is unknown.
     * Online, a sync of this launch that failed ends that wait: it may fail on every try, and the screens then show
     * what the device holds; offline, the wait goes on until the internet is back.
     */
    override fun invoke(): Flow<FirstDownloadProgress?> = combine(
        firstDownload.observeProgress(),
        accountRepository.observeAccount().map { it.awaitsFullSync }.distinctUntilChanged(),
        firstDownload.observeHasSyncFailed(),
        connectivityObserver.observe(),
    ) { progress, awaitsFullSync, hasSyncFailed, isOnline ->
        val awaitsCopy = awaitsFullSync && !(hasSyncFailed && isOnline)
        val pending = progress ?: FirstDownloadProgress(
            arrivedGroupCount = 0,
            groupCount = null,
            isWaitingForInternet = false,
        ).takeIf { awaitsCopy }
        pending?.copy(isWaitingForInternet = !isOnline)
    }.distinctUntilChanged()
}
