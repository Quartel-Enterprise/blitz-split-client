package com.quare.blitzsplit.core.watch.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveSignedOutOnPhone {
    // Why: 15.13 "You signed out on your phone", until the person connects again.
    operator fun invoke(): Flow<Boolean>
}
