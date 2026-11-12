package com.quare.blitzsplit.core.groups.domain.usecase

import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit

// Why: "Divisão padrão" (6.7), saved on this device and synced later with the group.
fun interface UpdateDefaultSplit {
    /*
     * Why: incomes are "By income"'s, by member id in minor units, saved for the group's members when the split is by
     * income: 0 removes one, and a member left out keeps theirs.
     */
    suspend operator fun invoke(
        groupId: String,
        defaultSplit: GroupDefaultSplit,
        incomes: Map<String, Long>,
    ): Result<Unit>
}
