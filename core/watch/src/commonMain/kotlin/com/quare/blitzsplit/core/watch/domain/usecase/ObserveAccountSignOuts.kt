package com.quare.blitzsplit.core.watch.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveAccountSignOuts {
    // Why: the user id each time this device's account ends, so the phone tells its watch to sign out too (15.13).
    operator fun invoke(): Flow<String>
}
