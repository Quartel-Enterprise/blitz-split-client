package com.quare.blitzsplit.core.account.domain.usecase

import com.quare.blitzsplit.core.model.AccountProvider
import kotlinx.coroutines.flow.Flow

fun interface ObserveBrowserSignInReturningToProfile {
    operator fun invoke(): Flow<AccountProvider?>
}
