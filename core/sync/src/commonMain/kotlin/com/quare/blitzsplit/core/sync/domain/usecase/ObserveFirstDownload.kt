package com.quare.blitzsplit.core.sync.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveFirstDownload {
    operator fun invoke(): Flow<Boolean>
}
