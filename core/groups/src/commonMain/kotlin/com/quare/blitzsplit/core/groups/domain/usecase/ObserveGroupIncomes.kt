package com.quare.blitzsplit.core.groups.domain.usecase

import kotlinx.coroutines.flow.Flow

// Why: the group's incomes for "By income", by member id in minor units, as its members saved them on any device.
fun interface ObserveGroupIncomes {
    operator fun invoke(groupId: String): Flow<Map<String, Long>>
}
