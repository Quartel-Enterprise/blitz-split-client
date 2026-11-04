package com.quare.blitzsplit.core.expenses.data.mapper

import com.quare.blitzsplit.core.expenses.data.dto.BillLinkResultDto
import com.quare.blitzsplit.core.expenses.data.dto.BillShareLinkDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillCommentDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillPartDto
import com.quare.blitzsplit.core.expenses.data.dto.SharedBillStatusDto
import com.quare.blitzsplit.core.expenses.domain.model.BillAttachmentKind
import com.quare.blitzsplit.core.expenses.domain.model.BillComment
import com.quare.blitzsplit.core.expenses.domain.model.BillCommentLike
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.BillShareLink
import com.quare.blitzsplit.core.expenses.domain.model.SharedBill
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillAttachment
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillComment
import com.quare.blitzsplit.core.expenses.domain.model.SharedBillPart
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.GroupCurrency

class SharedBillMapperImpl : SharedBillMapper {
    override fun toDomain(dto: BillShareLinkDto): BillShareLink = linkOf(dto.code, dto.visibility)

    override fun toDomain(dto: BillLinkResultDto): BillShareLink? = when (dto.status) {
        STATUS_OK -> dto.code?.let { code -> linkOf(code, dto.visibility) } ?: error("A link without its code")
        STATUS_NOT_FOUND -> null
        else -> error("Unknown bill link status ${dto.status}")
    }

    override fun toDomain(
        dto: SharedBillDto,
        imageUrls: Map<String, String>,
    ): SharedBill = when (dto.status) {
        STATUS_NOT_FOUND -> SharedBill.NotFound

        STATUS_MEMBERS_ONLY -> SharedBill.MembersOnly

        STATUS_OK -> {
            val bill = requireNotNull(dto.bill) { "A shared bill without the bill" }
            val groupId = requireNotNull(dto.groupId) { "A shared bill without its group" }
            SharedBill.Open(
                isMember = dto.isMember == true,
                groupId = groupId,
                groupName = dto.groupName.orEmpty(),
                currency = dto.currency?.let(
                    GroupCurrency::fromCodeOrNull,
                ) ?: error("Unknown currency ${dto.currency}"),
                expenseId = bill.id,
                description = bill.description,
                amountMinor = bill.amountMinor,
                spentAt = bill.spentAt,
                categoryId = bill.categoryId,
                // Why: a bill split by its items is stored as "amount"; the link doesn't carry its items.
                splitMode = SplitMode.fromCodeOrNull(bill.splitMode) ?: SplitMode.AMOUNT,
                payers = bill.payers.orEmpty().map(::partOf),
                shares = bill.shares.orEmpty().map(::partOf),
                // Why: a kind a newer version adds isn't drawn here.
                attachments = dto.attachments.orEmpty().mapNotNull { attachment ->
                    SharedBillAttachment(
                        id = attachment.id,
                        kind = BillAttachmentKind.fromCodeOrNull(attachment.kind) ?: return@mapNotNull null,
                        width = attachment.width,
                        height = attachment.height,
                        addedByName = attachment.addedByName,
                        addedAt = attachment.addedAt,
                        imageUrl = imageUrls[attachment.path],
                    )
                },
                comments = dto.comments.orEmpty().map { comment -> commentOf(comment, bill.id, groupId) },
            )
        }

        else -> error("Unknown shared bill status ${dto.status}")
    }

    override fun toDomain(dto: SharedBillStatusDto): SharedBillComment = when (dto.status) {
        STATUS_OK -> SharedBillComment.POSTED
        STATUS_NOT_FOUND -> SharedBillComment.NOT_FOUND
        STATUS_MEMBERS_ONLY -> SharedBillComment.MEMBERS_ONLY
        STATUS_NEEDS_NAME -> SharedBillComment.NEEDS_NAME
        else -> error("Unknown shared bill comment status ${dto.status}")
    }

    private fun linkOf(
        code: String,
        visibility: String?,
    ): BillShareLink = BillShareLink(code, BillLinkVisibility.fromCodeOrNull(visibility))

    private fun partOf(dto: SharedBillPartDto): SharedBillPart = SharedBillPart(
        memberId = dto.memberId,
        name = dto.name,
        amountMinor = dto.amountMinor,
    )

    // Why: the server keeps who wrote it by name only on a link's page, so nobody is this device's member there.
    private fun commentOf(
        dto: SharedBillCommentDto,
        expenseId: String,
        groupId: String,
    ): BillComment = BillComment(
        id = dto.id,
        expenseId = expenseId,
        groupId = groupId,
        authorUserId = null,
        authorMemberId = null,
        authorName = dto.authorName,
        threadId = dto.threadId,
        replyToName = dto.replyToName,
        body = dto.body,
        viaLink = dto.viaLink == true,
        postedAt = dto.postedAt,
        editedAt = dto.editedAt,
        isDeleted = dto.isDeleted == true,
        isMine = dto.isMine == true,
        likes = dto.likedBy.orEmpty().map { like ->
            BillCommentLike(
                memberId = null,
                name = like.name,
                isMine = like.isMine,
            )
        },
        isPending = false,
    )

    private companion object {
        const val STATUS_OK = "ok"
        const val STATUS_NOT_FOUND = "not_found"
        const val STATUS_MEMBERS_ONLY = "members_only"
        const val STATUS_NEEDS_NAME = "needs_name"
    }
}
