package com.quare.blitzsplit.core.groups.domain.usecase.impl

import com.quare.blitzsplit.core.groups.domain.model.GroupDefaultSplit
import com.quare.blitzsplit.core.groups.domain.repository.GroupIncomeRepository
import com.quare.blitzsplit.core.groups.domain.repository.GroupRepository
import com.quare.blitzsplit.core.groups.domain.usecase.UpdateDefaultSplit
import com.quare.blitzsplit.core.model.DefaultSplitMode
import com.quare.blitzsplit.core.utils.CurrentTimeMillis
import com.quare.blitzsplit.core.utils.suspendRunCatching

/*
 * Why: "Igual" with everyone in and no exception is what every group does without a default, so it is saved as none:
 * whoever joins later is in, with no notice to adjust. "By income" also saves the incomes it was typed with, which the
 * group's members then see and start from; another way leaves them as they are, for a bill split by income.
 */
class UpdateDefaultSplitUseCase(
    private val repository: GroupRepository,
    private val incomeRepository: GroupIncomeRepository,
    private val currentTimeMillis: CurrentTimeMillis,
) : UpdateDefaultSplit {
    override suspend fun invoke(
        groupId: String,
        defaultSplit: GroupDefaultSplit,
        incomes: Map<String, Long>,
    ): Result<Unit> = suspendRunCatching {
        val isEveryoneEqually = defaultSplit.isEveryoneEqually && defaultSplit.exceptions.isEmpty()
        val now = currentTimeMillis()
        repository
            .updateDefaultSplit(
                groupId = groupId,
                defaultSplit = defaultSplit.takeUnless { isEveryoneEqually },
                updatedAt = now,
            ).getOrThrow()
        if (defaultSplit.mode == DefaultSplitMode.INCOME && incomes.isNotEmpty()) {
            incomeRepository.saveIncomes(groupId, incomes, now).getOrThrow()
        }
    }
}
