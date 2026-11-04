package com.quare.blitzsplit.core.inappupdate.data.repository

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateAvailability
import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateDownloadState
import com.quare.blitzsplit.core.inappupdate.domain.repository.AppUpdateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// Why: the web always serves the latest version, and the jvm target only runs the tests.
internal class NoUpdateRepository : AppUpdateRepository {
    override suspend fun check(): UpdateAvailability = UpdateAvailability.NotAvailable

    override suspend fun startUpdate() {
        // Why: there is nothing to start without a store.
    }

    override fun observeDownload(): Flow<UpdateDownloadState> = emptyFlow()

    override suspend fun completeInstall() {
        // Why: nothing is ever downloaded.
    }
}
