package com.quare.blitzsplit.core.inappupdate.data.repository

import co.touchlab.kermit.Logger
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.quare.blitzsplit.core.inappupdate.CurrentActivityProvider
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateDownloadState
import com.quare.blitzsplit.core.inappupdate.domain.repository.AppUpdateRepository
import com.quare.blitzsplit.core.utils.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.android.play.core.install.model.UpdateAvailability as PlayUpdateAvailability

/*
 * Why: Google Play's flexible update, which downloads the new version while the app stays in use and installs it, with
 * a restart, once the app asks. A build not installed from Google Play (a debug one) only ever finds no update.
 */
internal class GooglePlayUpdateRepository(
    private val appUpdateManager: AppUpdateManager,
    private val activityProvider: CurrentActivityProvider,
) : AppUpdateRepository {
    private val downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    private val downloadingStatuses = setOf(InstallStatus.PENDING, InstallStatus.DOWNLOADING, InstallStatus.INSTALLING)

    private val listener = InstallStateUpdatedListener(::onInstallState)

    init {
        appUpdateManager.registerListener(listener)
    }

    override suspend fun check(): UpdateAvailability = suspendRunCatching {
        val info = appUpdateManager.appUpdateInfo.await()
        when {
            info.installStatus() == InstallStatus.DOWNLOADED -> UpdateAvailability.Downloaded

            info.isDownloading() -> UpdateAvailability.Downloading

            info.isFlexibleUpdateAvailable() -> UpdateAvailability.Available(
                versionName = null,
                storeUrl = null,
            )

            else -> UpdateAvailability.NotAvailable
        }
    }.getOrElse { throwable ->
        Logger.w(throwable) { "Failed to check Google Play for an update" }
        UpdateAvailability.CheckFailed
    }

    override suspend fun startUpdate() {
        suspendRunCatching {
            val activity = activityProvider.activity ?: return
            val info = appUpdateManager.appUpdateInfo.await()
            if (info.isFlexibleUpdateAvailable()) {
                appUpdateManager.startUpdateFlow(
                    info,
                    activity,
                    AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                )
            }
        }.onFailure { throwable ->
            Logger.w(throwable) { "Failed to start Google Play's update flow" }
        }
    }

    override fun observeDownload(): Flow<UpdateDownloadState> = downloadState

    override suspend fun completeInstall() {
        appUpdateManager.completeUpdate()
    }

    private fun onInstallState(state: InstallState) {
        downloadState.value = when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> UpdateDownloadState.Downloading(state.downloadPercent())
            InstallStatus.DOWNLOADED -> UpdateDownloadState.Downloaded
            InstallStatus.FAILED -> UpdateDownloadState.Failed
            else -> downloadState.value
        }
    }

    // Why: a download the flow started earlier, which Google Play reports as already in progress, not as available.
    private fun AppUpdateInfo.isDownloading(): Boolean = installStatus() in downloadingStatuses ||
        updateAvailability() == PlayUpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS

    private fun AppUpdateInfo.isFlexibleUpdateAvailable(): Boolean =
        updateAvailability() == PlayUpdateAvailability.UPDATE_AVAILABLE && isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

    private fun InstallState.downloadPercent(): Int {
        val total = totalBytesToDownload()
        return if (total > 0) (bytesDownloaded() * PERCENT_MAX / total).toInt() else 0
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

    private companion object {
        const val PERCENT_MAX = 100
    }
}
