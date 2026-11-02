package com.quare.blitzsplit.core.expenses.domain.usecase

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink

fun interface SetBillShareLink {
    // Why: turns the link on with who may open it, or changes who may; null when the bill is gone from the server.
    suspend operator fun invoke(
        expenseId: String,
        visibility: BillLinkVisibility,
    ): Result<BillShareLink?>
}
