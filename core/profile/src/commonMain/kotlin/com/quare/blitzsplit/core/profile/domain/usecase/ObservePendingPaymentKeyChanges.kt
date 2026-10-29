package com.quare.blitzsplit.core.profile.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObservePendingPaymentKeyChanges {
    operator fun invoke(): Flow<Boolean>
}
