package com.quare.blitzsplit.core.sync.exit

import com.quare.blitzsplit.core.expenses.domain.model.GroupBalances
import com.quare.blitzsplit.core.groups.domain.model.Group

// Why: the group "Sair do grupo" and "Apagar grupo" are about, as the device has it now.
data class GroupExitTarget(
    val group: Group,
    val balances: GroupBalances,
    // Why: "as 14 despesas" of "Apagar grupo".
    val billCount: Int,
)
