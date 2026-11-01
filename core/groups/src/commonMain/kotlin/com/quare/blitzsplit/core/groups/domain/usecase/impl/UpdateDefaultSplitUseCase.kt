package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateDefaultSplit
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.utils.CurrentTimeMillis

/*
 * Why: "Igual" with everyone in is what every group does without a default, so it is saved as none: whoever joins
 * later is in, with no notice to adjust. Only "Pela renda" keeps the incomes it was typed with.
 */
class UpdateDefaultSplitUseCase(
    private val repository: GroupRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : UpdateDefaultSplit {
    override suspend fun invoke(
        groupId: String,
        defaultSplit: GroupDefaultSplit,
        incomes: Map<String, Long>,
    ): Result<Unit> {
        val isEveryoneEqually =
            defaultSplit.mode == DefaultSplitMode.EQUAL && defaultSplit.weights.values.all { it > 0 }
        return repository.updateDefaultSplit(
            groupId = groupId,
            defaultSplit = defaultSplit.takeUnless { isEveryoneEqually },
            incomes = if (defaultSplit.mode == DefaultSplitMode.INCOME) incomes.filterValues { it > 0 } else emptyMap(),
            updatedAt = currentTimeMillis(),
        )
    }
}
