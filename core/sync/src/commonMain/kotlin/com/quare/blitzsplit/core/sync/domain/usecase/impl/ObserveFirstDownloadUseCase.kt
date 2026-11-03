package com.quare.blitzsplit.core.sync.domain.usecase.impl

import com.quare.blitzsplit.core.sync.FirstDownload
import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.core.sync.domain.usecase.ObserveFirstDownload
import kotlinx.coroutines.flow.Flow

class ObserveFirstDownloadUseCase(
    private val firstDownload: FirstDownload,
) : ObserveFirstDownload {
    override fun invoke(): Flow<FirstDownloadProgress?> = firstDownload.observeProgress()
}
