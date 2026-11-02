package com.quare.blitzsplit.feature.billdetail.presentation.model

import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor

// Why: one line of "Histórico", "Ana trocou a categoria · 22:41"; each UI says "Você" for actorIsMe.
data class BillHistoryEntry(
    val id: String,
    val kind: BillHistoryKind,
    val actorName: String?,
    val actorInitials: String,
    val actorColor: AvatarColor,
    val actorPhotoUrl: String?,
    val actorIsMe: Boolean,
    val occurredAt: Long,
    val changes: List<BillHistoryChange>,
    // Why: CREATED's note, "Igual entre 5 · R$ 312,00", as the bill was created.
    val amountMinor: Long?,
    val splitMode: SplitMode?,
    val sharesCount: Int?,
    // Why: how many photos ATTACHMENTS_ADDED gathers.
    val attachmentCount: Int,
    // Why: the attachment is the receipt the bill was read from ("Cupom").
    val isReceipt: Boolean,
    // Why: who LINK_CHANGED opened the link to; null once it was turned off.
    val linkVisibility: BillLinkVisibility?,
    // Why: done on this device and not up yet: the orange dot.
    val isPending: Boolean,
    // Why: "Desfazer", only on the latest edit while the bill is still as it left it.
    val canUndo: Boolean,
)
