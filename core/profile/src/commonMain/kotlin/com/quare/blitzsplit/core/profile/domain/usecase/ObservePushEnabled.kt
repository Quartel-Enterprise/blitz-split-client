package com.quare.blitzsplit.core.profile.domain.usecase

import kotlinx.coroutines.flow.Flow

// Why: Notifications' "Push notifications" switch on this device, apart from the system's permission.
fun interface ObservePushEnabled {
    operator fun invoke(): Flow<Boolean>
}
