package com.quare.blitzsplit.core.expenses.domain.model

/*
 * Why: a bill split by its items (7.5), kept inside the bill like its payers and shares, so the shares derived from
 * the items stay one Last-Write-Wins row with them. The shares still hold each one's total, so a version that doesn't
 * know items reads the bill as split by amounts.
 */
data class BillItems(
    val items: List<BillItem>,
    // Why: "Serviço (10%)", proportional to what each one consumed; 0 for a bill without it.
    val servicePercent: Long,
    // Why: "Couvert", the same for everyone at the table, per person in the minor unit; 0 for none.
    val couvertMinor: Long,
    // Why: who is at the table and pays the couvert: the group's members when the items were split, in its order.
    val couvertMemberIds: List<String>,
    // Why: "Dividir a diferença" (7.5d), shared in proportion to what each one consumed, as the service is.
    val extraMinor: Long,
    // Why: the items came from a receipt read (7.1e), whose total "o cupom diz"; typed by hand otherwise.
    val isFromReceipt: Boolean,
)
