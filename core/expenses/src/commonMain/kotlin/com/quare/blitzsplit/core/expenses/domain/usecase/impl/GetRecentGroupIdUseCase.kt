package com.quare.blitzsplit.core.expenses.domain.usecase.impl

import com.quare.blitzsplit.core.expenses.domain.usecase.GetRecentGroupId
import com.quare.blitzsplit.core.expenses.domain.usecase.ObserveGroupsByActivity
import kotlinx.coroutines.flow.first

// Why: the first of the home's order, null while the device has no group.
class GetRecentGroupIdUseCase(
    private val observeGroupsByActivity: ObserveGroupsByActivity,
) : GetRecentGroupId {
    override suspend fun invoke(): String? = observeGroupsByActivity()
        .first()
        .firstOrNull()
        ?.id
}
