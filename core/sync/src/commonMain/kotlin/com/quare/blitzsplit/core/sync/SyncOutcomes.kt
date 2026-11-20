package com.quare.blitzsplit.core.sync

import com.quare.blitzsplit.core.provider.crashlytics.CrashReporter
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import software.quare.connectivity.NetworkConnectivityObserver
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.seconds

/*
 * Why: Supabase tries a failed token refresh again every 10 seconds, so a session back within this came back on its
 * own (the network waking up with the app); one still refreshing after it is a server that doesn't answer.
 */
internal val PUT_OFF_GRACE = 30.seconds

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
    private val lastSyncRecorder: LastSyncRecorder,
    private val scope: CoroutineScope,
) {
    private val mutableLastSyncFailed = MutableStateFlow(false)

    @Volatile
    private var isLastSyncPutOff = false
    private var putOffTimeout: Job? = null
    private var hasReportedFailure = false

    // Why: the timeout of a put-off sync ends apart from the sync lock, so the two never interleave.
    private val mutex = Mutex()

    val lastSyncFailed: StateFlow<Boolean> = mutableLastSyncFailed

    // Why: the last sync failed or was put off for the session's refresh, so the token coming back runs it again.
    val lastSyncWentThrough: Boolean
        get() = !mutableLastSyncFailed.value && !isLastSyncPutOff

    suspend fun onSyncEnded(result: Result<Unit>) {
        mutex.withLock { endSync(result) }
    }

    /*
     * Why: the session came back, or ended, so the refresh a put-off sync waited for is over: the sync that runs next
     * decides the outcome, never the grace of a session no longer refreshing, which would fail it midway.
     */
    suspend fun onSessionSettled() {
        mutex.withLock { putOffTimeout?.cancel() }
    }

    private suspend fun endSync(result: Result<Unit>) {
        val failure = result.exceptionOrNull()
        /*
         * Why: a sync put off for the session's refresh changes nothing the screens show for a while: not "No response
         * from the server", whose retry then said "Syncing…" on every return to the front (beta feedback), nor the
         * first download's wait. Only once the session stays refreshing past the grace does it count as failed. The
         * grace runs from the first sync put off, so syncs put off one after another never push it back.
         */
        isLastSyncPutOff = failure?.isPutOffForSession() == true
        if (failure != null && isLastSyncPutOff) {
            if (putOffTimeout?.isActive != true) {
                putOffTimeout = scope.launch {
                    delay(PUT_OFF_GRACE)
                    mutex.withLock { recordOutcome(failure) }
                }
            }
        } else {
            putOffTimeout?.cancel()
            recordOutcome(failure)
        }
    }

    private suspend fun recordOutcome(failure: Throwable?) {
        mutableLastSyncFailed.value = failure != null
        if (failure != null) firstDownload.syncFailed() else firstDownload.syncSucceeded()
        // Why: a failed write of the time never fails the sync it records.
        if (failure == null) suspendRunCatching { lastSyncRecorder.record() }
        when {
            failure == null -> hasReportedFailure = false
            !hasReportedFailure -> hasReportedFailure = report(failure)
        }
    }

    // Why: whether it told, so a failure it left out never counts as the streak's report.
    private suspend fun report(failure: Throwable): Boolean {
        if (failure.isExpectedFailure() || !connectivityObserver.observe().first()) return false
        crashReporter.recordException(SyncFailure(failure))
        return true
    }
}
