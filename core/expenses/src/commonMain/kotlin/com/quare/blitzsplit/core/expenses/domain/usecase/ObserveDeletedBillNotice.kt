package com.quare.blitzsplit.core.expenses.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface ObserveDeletedBillNotice {
    // Why: whether the group's Despesas do grupo owes "Esta conta foi apagada".
    operator fun invoke(groupId: String): Flow<Boolean>
}
