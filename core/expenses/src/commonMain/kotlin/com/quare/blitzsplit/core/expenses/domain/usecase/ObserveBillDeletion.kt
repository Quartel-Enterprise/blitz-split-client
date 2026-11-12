package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.BillDeletion
import kotlinx.coroutines.flow.Flow

fun interface ObserveBillDeletion {
    /*
     * Why: "Apagar esta conta?" worded from the bill as it is now, from any list that offers the bill's ⋮ (the group's
     * Despesas, Início, "Todas as despesas"), the payment it unsettles included; null once the bill is gone.
     */
    operator fun invoke(
        groupId: String,
        expenseId: String,
    ): Flow<BillDeletion?>
}
