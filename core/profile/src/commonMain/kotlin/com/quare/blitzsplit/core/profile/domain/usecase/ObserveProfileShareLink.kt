package com.quare.blitzsplit.core.profile.domain.usecase

import kotlinx.coroutines.flow.Flow

// Why: the account's profile link, blitzsplit.com/u/<code>; null while no account owns this device's profile.
fun interface ObserveProfileShareLink {
    operator fun invoke(): Flow<String?>
}
