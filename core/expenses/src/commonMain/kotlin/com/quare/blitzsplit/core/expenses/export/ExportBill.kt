package com.quare.blitzsplit.core.expenses.export

import com.quare.blitzsplit.core.expenses.domain.model.Category
import com.quare.blitzsplit.core.expenses.domain.model.PayingMethod
import com.quare.blitzsplit.core.model.GroupCurrency
import kotlinx.datetime.LocalDate

data class ExportBill(
    val id: String,
    val date: LocalDate,
    // Why: the instant, which the JSON keeps beside the day.
    val spentAt: Long,
    val description: String,
    val category: Category,
    val amountMinor: Long,
    val currency: GroupCurrency,
    // Why: who paid how much, and each one's part of the split, in the bill's currency.
    val payers: List<ExportAmount>,
    val shares: List<ExportAmount>,
    // Why: the exporter's own "Paid with", private, so it only ever comes from this device's user; null for none.
    val payingMethod: PayingMethod?,
) {
    // Why: null when the member paid nothing of it, so the cell stays empty rather than reading 0.
    fun findPaidMinorOrNull(memberId: String): Long? = payers
        .filter { it.memberId == memberId }
        .takeIf { it.isNotEmpty() }
        ?.sumOf { it.amountMinor }

    fun findShareMinorOrNull(memberId: String): Long? = shares
        .filter { it.memberId == memberId }
        .takeIf { it.isNotEmpty() }
        ?.sumOf { it.amountMinor }
}
