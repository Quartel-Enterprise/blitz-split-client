package com.quare.blitzsplit.core.inappupdate.domain.usecase

import com.quare.blitzsplit.core.inappupdate.domain.model.UpdateDownloadState
import kotlinx.coroutines.flow.Flow

fun interface ObserveUpdateDownloadState {
    operator fun invoke(): Flow<UpdateDownloadState>
}
