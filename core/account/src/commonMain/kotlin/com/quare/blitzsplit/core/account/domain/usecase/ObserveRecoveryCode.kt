package com.quare.blitzsplit.core.account.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveRecoveryCode {
    operator fun invoke(): Flow<String?>
}
