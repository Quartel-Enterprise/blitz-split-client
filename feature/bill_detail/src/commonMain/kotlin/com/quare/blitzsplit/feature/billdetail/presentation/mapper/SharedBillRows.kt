package com.quare.blitzsplit.feature.billdetail.presentation.mapper

import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.SpentDay
import com.quare.blitzsplit.core.groups.domain.model.Initials
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillAttachmentTile
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import kotlinx.datetime.TimeZone

/*
 * Why: a bill's link page draws the bill as Conta does, by the names the server sent: nobody is this device's user
 * there, and someone who left the group has no name ("Ex-membro").
 */
internal fun payersOf(bill: SharedBill.Open): List<BillPayer> = bill.payers.map { payer ->
    BillPayer(
        name = payer.name,
        initials = payer.name?.let { Initials.ofMember(it, isMe = false) }.orEmpty(),
        avatarColor = AvatarColor.default,
        photoUrl = null,
        isMe = false,
    )
}

// Why: whoever has a part, in the bill's order, then whoever only paid.
internal fun peopleOf(bill: SharedBill.Open): List<BillShareRow> {
    val paid = bill.payers.associate { it.memberId to it.amountMinor }
    val shared = bill.shares.associate { it.memberId to it.amountMinor }
    val names = (bill.shares + bill.payers).associate { it.memberId to it.name }
    return (bill.shares.map { it.memberId } + bill.payers.map { it.memberId })
        .distinct()
        .map { memberId ->
            val name = names[memberId]
            BillShareRow(
                memberId = memberId,
                name = name,
                initials = name?.let { Initials.ofMember(it, isMe = false) }.orEmpty(),
                avatarColor = AvatarColor.default,
                photoUrl = null,
                isMe = false,
                hasJoined = true,
                paidMinor = paid[memberId],
                shareMinor = shared[memberId],
            )
        }
}

// Why: as Conta's "Anexos", the receipt first, then "Foto 1", "Foto 2"; nothing is removed from a link's page.
internal fun sharedTilesOf(
    attachments: List<SharedBillAttachment>,
    nowMillis: Long,
    timeZone: TimeZone,
): List<BillAttachmentTile> {
    var photoNumber = 0
    return attachments.map { attachment ->
        val isReceipt = attachment.kind == BillAttachmentKind.RECEIPT
        BillAttachmentTile(
            id = attachment.id,
            isReceipt = isReceipt,
            localImage = null,
            imageUrl = attachment.imageUrl,
            width = attachment.width,
            height = attachment.height,
            photoNumber = if (isReceipt) null else ++photoNumber,
            adderName = attachment.addedByName,
            isAddedByMe = false,
            addedAt = attachment.addedAt,
            addedDay = SpentDay.relativeDayOf(
                epochMillis = attachment.addedAt,
                nowMillis = nowMillis,
                timeZone = timeZone,
            ),
            isPending = false,
            canRemove = false,
        )
    }
}
