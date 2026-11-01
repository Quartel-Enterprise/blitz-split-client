package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit

// Why: "Divisão padrão" (6.7), saved on this device and synced later with the group.
fun interface UpdateDefaultSplit {
    // Why: incomes are "Pela renda"'s, by member id in minor units, which stay on this device.
    suspend operator fun invoke(
        groupId: String,
        defaultSplit: GroupDefaultSplit,
        incomes: Map<String, Long>,
    ): Result<Unit>
}
