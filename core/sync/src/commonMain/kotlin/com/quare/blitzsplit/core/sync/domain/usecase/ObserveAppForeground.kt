package com.quare.blitzsplit.core.sync.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveAppForeground {
    operator fun invoke(): Flow<Boolean>
}
