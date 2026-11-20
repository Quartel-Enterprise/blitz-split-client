package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.BalanceSnapshot
import com.quare.blitzsplit.core.expenses.domain.model.GroupActivity
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveBalanceSnapshot
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

// Why: the groups in the home's order and their money from one read of each, as Saldos and Início read them.
class ObserveBalanceSnapshotUseCase(
    private val observeGroups: ObserveGroups,
    private val observeGroupsMoney: ObserveGroupsMoney,
) : ObserveBalanceSnapshot {
    override fun invoke(): Flow<BalanceSnapshot> = combine(observeGroups(), observeGroupsMoney()) { groups, money ->
        BalanceSnapshot.of(
            groups = GroupActivity.sortMostRecentFirst(groups, money),
            money = money,
        )
    }.distinctUntilChanged()
}
