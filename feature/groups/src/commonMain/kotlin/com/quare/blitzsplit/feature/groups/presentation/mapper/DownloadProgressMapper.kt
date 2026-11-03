package com.quare.blitzsplit.feature.groups.presentation.mapper

import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import com.quare.blitzsplit.feature.groups.presentation.model.DownloadProgress

internal fun FirstDownloadProgress.toDownloadProgress(): DownloadProgress = DownloadProgress(
    arrivedGroupCount = arrivedGroupCount,
    groupCount = groupCount,
    isWaitingForInternet = isWaitingForInternet,
)
