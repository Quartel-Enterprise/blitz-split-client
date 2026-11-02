package com.quare.blitzsplit.core.expenses.domain.model

import com.quare.blitzsplit.core.model.GroupCurrency

// Why: what a bill's link shows (7.3h) to whoever opens it, signed in or not, read from the server each time.
sealed interface SharedBill {
    // Why: a code that is no link (anymore), or a bill since deleted.
    data object NotFound : SharedBill

    // Why: "Só quem está na conta", opened by someone outside the group or not signed in.
    data object MembersOnly : SharedBill

    data class Open(
        // Why: someone in the group, who sees their own part and the history in the app.
        val isMember: Boolean,
        val groupId: String,
        val groupName: String,
        val currency: GroupCurrency,
        val expenseId: String,
        val description: String,
        val amountMinor: Long,
        val spentAt: Long,
        val categoryId: String?,
        val splitMode: SplitMode,
        val payers: List<SharedBillPart>,
        val shares: List<SharedBillPart>,
        // Why: the receipt first, then the photos in the order they were added.
        val attachments: List<SharedBillAttachment>,
        // Why: in the order they were written, as the server keeps them.
        val comments: List<BillComment>,
    ) : SharedBill
}
