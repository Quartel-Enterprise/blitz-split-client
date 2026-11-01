package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.model.PeopleBalances
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsByActivity
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsMoney
import com.quare.blitzsplit.core.expenses.domain.usecase.ObservePeopleBalances
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObservePeopleBalancesUseCase(
    private val observeGroupsByActivity: ObserveGroupsByActivity,
    private val observeGroupsMoney: ObserveGroupsMoney,
) : ObservePeopleBalances {
    override fun invoke(): Flow<PeopleBalances> = combine(
        observeGroupsByActivity(),
        observeGroupsMoney(),
        PeopleBalances::of,
    ).distinctUntilChanged()
}
