package com.quare.blitzsplit.feature.categorymap.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.Expense

// Why: the bills of the group "Editar categoria" is in, read once its form opens: "usada em 3 contas" (9.4).
data class GroupBills(
    val groupId: String,
    val expenses: List<Expense>,
)
