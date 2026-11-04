package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.provider.crashlytics.CrashReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import software.quare.connectivity.NetworkConnectivityObserver

/*
 * Why: what each sync's end changes: the failed sync's badge, the first download's wait, and Crashlytics. Offline, or
 * with a request that got no answer, a failed sync is expected; online it is a change the server keeps refusing or a
 * step that broke, which fails every try and only Crashlytics tells: once, until a sync gets through again. Called
 * under SyncCoordinator's sync lock, one sync at a time.
 */
internal class SyncOutcomes(
    private val firstDownload: FirstDownload,
    private val crashReporter: CrashReporter,
    private val connectivityObserver: NetworkConnectivityObserver,
) {
    private val mutableLastSyncFailed = MutableStateFlow(false)
    private var hasReportedFailure = false

    val lastSyncFailed: StateFlow<Boolean> = mutableLastSyncFailed

    suspend fun onSyncEnded(result: Result<Unit>) {
        mutableLastSyncFailed.value = result.isFailure
        if (result.isFailure) firstDownload.syncFailed() else firstDownload.syncSucceeded()
        val failure = result.exceptionOrNull()
        when {
            failure == null -> hasReportedFailure = false
            !hasReportedFailure -> hasReportedFailure = report(failure)
        }
    }

    // Why: whether it told, so a failure it left out never counts as the streak's report.
    private suspend fun report(failure: Throwable): Boolean {
        if (failure.isConnectionFailure() || !connectivityObserver.observe().first()) return false
        crashReporter.recordException(SyncFailure(failure))
        return true
    }
}
