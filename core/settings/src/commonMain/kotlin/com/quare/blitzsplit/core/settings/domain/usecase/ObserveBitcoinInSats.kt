package com.quare.blitzsplit.core.settings.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveBitcoinInSats {
    operator fun invoke(): Flow<Boolean>
}
