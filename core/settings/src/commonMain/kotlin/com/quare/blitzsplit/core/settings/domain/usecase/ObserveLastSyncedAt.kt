package com.quare.blitzsplit.core.settings.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveLastSyncedAt {
    // Why: the watch's "Updated at 14:32", when a sync last went through on this device; null until one did.
    operator fun invoke(): Flow<Long?>
}
