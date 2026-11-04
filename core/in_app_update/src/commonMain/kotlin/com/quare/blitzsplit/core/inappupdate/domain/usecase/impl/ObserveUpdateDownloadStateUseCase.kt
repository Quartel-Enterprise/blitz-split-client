package com.quare.blitzsplit.core.inappupdate.domain.usecase.impl

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateDownloadState
import com.quare.blitzsplit.core.inappupdate.domain.repository.AppUpdateRepository
import com.quare.blitzsplit.core.inappupdate.domain.usecase.ObserveUpdateDownloadState
import kotlinx.coroutines.flow.Flow

class ObserveUpdateDownloadStateUseCase(
    private val repository: AppUpdateRepository,
) : ObserveUpdateDownloadState {
    override fun invoke(): Flow<UpdateDownloadState> = repository.observeDownload()
}
