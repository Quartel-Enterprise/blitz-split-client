package com.quare.blitzsplit.core.account.domain.usecase

import com.quare.blitzsplit.core.account.domain.model.AbandonedBrowserSignIn
import kotlinx.coroutines.flow.Flow

// Why: a browser closed without finishing sends no callback, so nothing else ends its wait.
fun interface ObserveAbandonedBrowserSignIn {
    operator fun invoke(): Flow<AbandonedBrowserSignIn>
}
