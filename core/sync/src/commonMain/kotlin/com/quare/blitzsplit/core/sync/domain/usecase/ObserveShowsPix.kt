package com.quare.blitzsplit.core.sync.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveShowsPix {
    // Why: the Pix rule (docs/product/general.md), live: a key added or a Brazilian group joined turns it on at once.
    operator fun invoke(): Flow<Boolean>
}
