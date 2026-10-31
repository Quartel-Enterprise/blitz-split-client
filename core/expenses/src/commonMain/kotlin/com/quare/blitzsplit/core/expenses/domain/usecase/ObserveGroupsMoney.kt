package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.GroupMoney
import kotlinx.coroutines.flow.Flow

fun interface ObserveGroupsMoney {
    /*
     * Why: every group's bills and payments by group id, from one read of each table, so the home doesn't open two
     * queries per group again on every change. A group with neither is not in the map.
     */
    operator fun invoke(): Flow<Map<String, GroupMoney>>
}
