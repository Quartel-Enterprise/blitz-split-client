package com.quare.blitzsplit.core.account.domain.usecase

import com.quare.blitzsplit.core.account.domain.model.AccountSnapshot
import kotlinx.coroutines.flow.Flow

fun interface ObserveAccount {
    operator fun invoke(): Flow<AccountSnapshot>
}
