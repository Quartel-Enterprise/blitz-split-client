package com.quare.blitzsplit.core.groups.domain.usecase

import kotlinx.coroutines.flow.Flow

// Why: whether the device has a group, settled ones included, which is what "Buscar" (5.2) needs to look in.
fun interface ObserveHasGroups {
    operator fun invoke(): Flow<Boolean>
}
