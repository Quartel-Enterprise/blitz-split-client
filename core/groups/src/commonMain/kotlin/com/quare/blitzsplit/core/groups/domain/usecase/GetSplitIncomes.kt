package com.quare.blitzsplit.core.groups.domain.usecase

// Why: the incomes this device typed for the group's "Pela renda", by member id in minor units.
fun interface GetSplitIncomes {
    suspend operator fun invoke(groupId: String): Result<Map<String, Long>>
}
