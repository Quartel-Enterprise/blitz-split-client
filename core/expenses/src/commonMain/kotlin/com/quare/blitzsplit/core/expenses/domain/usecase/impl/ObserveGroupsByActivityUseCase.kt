package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.GroupActivity
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
    // Why: in the home's order (GroupActivity.compareMostRecentFirst), each group's activity read once per emission.
    override fun invoke(): Flow<List<Group>> = combine(
        observeGroups(),
        observeGroupsMoney(),
        GroupActivity::sortMostRecentFirst,
    )
}
