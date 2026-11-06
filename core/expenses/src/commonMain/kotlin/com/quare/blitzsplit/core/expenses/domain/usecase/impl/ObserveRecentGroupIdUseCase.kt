package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsByActivity
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveRecentGroupId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

// Why: the first of the home's order, emitted again only when another group takes its place or the last one goes.
class ObserveRecentGroupIdUseCase(
    private val observeGroupsByActivity: ObserveGroupsByActivity,
) : ObserveRecentGroupId {
    override fun invoke(): Flow<String?> = observeGroupsByActivity()
        .map { groups -> groups.firstOrNull()?.id }
        .distinctUntilChanged()
}
