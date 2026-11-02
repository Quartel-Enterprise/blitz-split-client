package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.ExpenseShare
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode

// Why: the bill as "Desfazer" saves it, the latest edit's "before" over the bill as it is now.
internal data class BillUndo(
    val description: String,
    val amountMinor: Long,
    val payers: List<ExpensePayer>,
    val splitMode: SplitMode,
    val shares: List<ExpenseShare>,
    val categoryId: String?,
    val spentAt: Long,
)
