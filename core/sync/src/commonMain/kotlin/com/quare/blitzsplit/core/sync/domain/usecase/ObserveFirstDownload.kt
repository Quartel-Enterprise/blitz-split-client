package com.quare.blitzsplit.core.sync.domain.usecase

import com.quare.blitzsplit.core.sync.domain.model.FirstDownloadProgress
import kotlinx.coroutines.flow.Flow

// Why: null while no first download is filling the device.
fun interface ObserveFirstDownload {
    operator fun invoke(): Flow<FirstDownloadProgress?>
}
