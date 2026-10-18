package com.quare.blitzsplit.core.account.domain.usecase

import com.quare.blitzsplit.core.model.AccountStatus
import kotlinx.coroutines.flow.Flow

fun interface ObserveAccountStatus {
    operator fun invoke(): Flow<AccountStatus>
}
