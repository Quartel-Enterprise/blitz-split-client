package com.quare.blitzsplit.core.sync.exit

import com.quare.blitzsplit.core.expenses.domain.model.CurrencyBalances
import com.quare.blitzsplit.core.groups.domain.model.Group

// Why: the group "Sair do grupo" and "Apagar grupo" are about, as the device has it now.
data class GroupExitTarget(
    val group: Group,
    // Why: one balance per currency the group has money in: leaving or deleting needs every one of them settled.
    val balances: List<CurrencyBalances>,
    // Why: "as 14 despesas" of "Apagar grupo".
    val billCount: Int,
)
