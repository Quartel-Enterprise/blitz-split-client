package com.quare.blitzsplit.core.groups.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// Why: whether the device has a group, settled ones included, which is what "Buscar" (5.2) needs to look in.
fun ObserveGroups.hasGroups(): Flow<Boolean> = invoke()
    .map { groups -> groups.isNotEmpty() }
    .distinctUntilChanged()
