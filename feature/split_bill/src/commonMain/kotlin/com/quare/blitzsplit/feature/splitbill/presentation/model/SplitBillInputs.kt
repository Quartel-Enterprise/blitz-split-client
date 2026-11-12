package com.quare.blitzsplit.feature.splitbill.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.ExpensePayer
import com.quare.blitzsplit.core.expenses.domain.model.SplitEntry
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency

/*
 * Why: everything the person can change in "Rachar conta" or "Editar conta", as one value compared with how the form
 * opened, so a form changed back to how it opened is equal again and closing it asks nothing. Unlike BillInputs, it
 * always exists (that one is null while the form can't be saved) and it reads the description and the category. What
 * is only being typed in a sheet isn't part of it until the sheet's "Pronto".
 */
data class SplitBillInputs(
    val groupId: String,
    val amountMinor: Long?,
    val description: String,
    val currency: GroupCurrency,
    val payers: List<ExpensePayer>,
    val spentAt: Long,
    val splitMode: SplitMode,
    val splitEntries: List<SplitEntry>,
    val categoryId: String?,
    // Why: a receipt's photo read or being read is input the person would have to take again.
    val hasReceipt: Boolean,
    val splitsAsDefault: Boolean,
)
