package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.GroupActivity
import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import com.quare.blitzsplit.core.groups.domain.usecase.ObserveGroups
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/*
 * Why: the groups in the home's order and their money from one read of each, so the order and the balances always come
 * from the same bills and payments, and Room is watched once.
 */
class ObservePeopleBalancesUseCase(
    private val observeGroups: ObserveGroups,
    private val observeGroupsMoney: ObserveGroupsMoney,
) : ObservePeopleBalances {
    override fun invoke(): Flow<PeopleBalances> = combine(observeGroups(), observeGroupsMoney()) { groups, money ->
        PeopleBalances.of(
            groups = GroupActivity.sortMostRecentFirst(groups, money),
            money = money,
        )
    }.distinctUntilChanged()
}
