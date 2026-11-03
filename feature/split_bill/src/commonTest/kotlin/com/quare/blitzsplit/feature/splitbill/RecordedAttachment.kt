package com.quare.blitzsplit.feature.splitbill

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind

// Why: one call to AddBillAttachment as ReceiptViewModelTest records it, the photo as a list so it compares by content.
internal data class RecordedAttachment(
    val expenseId: String,
    val groupId: String,
    val kind: BillAttachmentKind,
    val source: List<Byte>,
    val addedByMemberId: String?,
)
