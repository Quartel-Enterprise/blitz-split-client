package com.quare.blitzsplit.core.settings.domain.usecase

import kotlinx.coroutines.flow.Flow

// Why: Profile's "Share usage data", on this device.
fun interface ObserveUsageDataSharing {
    operator fun invoke(): Flow<Boolean>
}
