package com.quare.blitzsplit.core.account.domain.usecase

import com.quare.blitzsplit.core.model.RecoveryCodePrompt
import kotlinx.coroutines.flow.Flow

fun interface ObserveRecoveryCodePrompt {
    operator fun invoke(): Flow<RecoveryCodePrompt>
}
