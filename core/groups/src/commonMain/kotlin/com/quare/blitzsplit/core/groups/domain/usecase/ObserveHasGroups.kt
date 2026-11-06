package com.quare.blitzsplit.core.groups.domain.usecase

import kotlinx.coroutines.flow.Flow

// Why: whether the device has a group, settled ones included, which is what "Rachar conta" needs to split in.
fun interface ObserveHasGroups {
    operator fun invoke(): Flow<Boolean>
}
