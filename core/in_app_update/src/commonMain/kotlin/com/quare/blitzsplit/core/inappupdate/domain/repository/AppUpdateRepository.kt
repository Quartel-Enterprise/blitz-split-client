package com.quare.blitzsplit.core.inappupdate.domain.repository

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateDownloadState
import kotlinx.coroutines.flow.Flow

/*
 * Why: where this platform's new versions come from, bound per platform: Google Play on Android, the App Store on iOS
 * and macOS, none on the web.
 */
interface AppUpdateRepository {
    suspend fun check(): UpdateAvailability

    // Why: Google Play's flexible flow, its own dialog asking first; the App Store has none.
    suspend fun startUpdate()

    fun observeDownload(): Flow<UpdateDownloadState>

    // Why: installs what Google Play downloaded, which restarts the app.
    suspend fun completeInstall()
}
