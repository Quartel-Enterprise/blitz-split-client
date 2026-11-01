package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.GroupActivity
import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsByActivity
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.groups.domain.model.Group
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveGroupsByActivityUseCase(
    private val observeGroups: ObserveGroups,
    private val observeGroupsMoney: ObserveGroupsMoney,
) : ObserveGroupsByActivity {
    // Why: a stable sort, so the groups' own order (the latest change first) breaks a tie, as on the home.
    override fun invoke(): Flow<List<Group>> = combine(observeGroups(), observeGroupsMoney()) { groups, money ->
        groups.sortedByDescending { group ->
            val ofGroup = money[group.id] ?: GroupMoney.NONE
            GroupActivity.latestAtOf(
                groupUpdatedAt = group.updatedAt,
                expenses = ofGroup.expenses,
                settlements = ofGroup.settlements,
            )
        }
    }
}
