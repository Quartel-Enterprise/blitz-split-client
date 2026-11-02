package com.quare.blitzsplit.android.feature.billdetail

import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.BillLinkVisibility
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillAttachmentTile
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillCommentThread
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryChange
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryDay
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryEntry
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryKind
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillHistoryPart
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillReplyTarget
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.MyPartNote

private const val PREVIEW_SPENT_AT = 1_761_683_400_000L
private const val PREVIEW_TOTAL_MINOR = 64_200L
private const val PREVIEW_SHARE_MINOR = 12_840L
private const val PREVIEW_NOW = 1_761_726_600_000L

internal fun createPreviewBillDetailState(
    isMePayer: Boolean = false,
    isPending: Boolean = false,
    // Why: answering Diego offline, with an earlier reply of this device's user still in the queue.
    isReplyingOffline: Boolean = false,
): BillDetailUiState {
    val payerId = if (isMePayer) "me" else "bruno"
    val people = listOf(
        createPreviewRow("me", "Pierre", "P", AvatarColor.PEACH, isMe = true),
        createPreviewRow("bruno", "Bruno", "BR", AvatarColor.GREEN),
        createPreviewRow("ana", "Ana", "AN", AvatarColor.BLUE),
        createPreviewRow("carla", "Carla", "CA", AvatarColor.LILAC),
        createPreviewRow("diego", "Diego", "DI", AvatarColor.SAND, hasJoined = false),
        createPreviewRow("elisa", "Elisa", "EL", AvatarColor.BLUE),
    ).map { row ->
        row.copy(
            paidMinor = PREVIEW_TOTAL_MINOR.takeIf { row.memberId == payerId },
            shareMinor = PREVIEW_SHARE_MINOR.takeIf { row.memberId != "elisa" },
        )
    }
    return BillDetailUiState(
        groupName = "Viagem Floripa",
        currency = GroupCurrency.BRL,
        description = "Jantar no Ostradamus",
        totalMinor = PREVIEW_TOTAL_MINOR,
        spentAt = PREVIEW_SPENT_AT,
        spentDay = RelativeDay.YESTERDAY,
        payers = listOf(
            BillPayer(
                name = if (isMePayer) "Pierre" else "Bruno",
                initials = if (isMePayer) "P" else "BR",
                avatarColor = if (isMePayer) AvatarColor.PEACH else AvatarColor.GREEN,
                photoUrl = null,
                isMe = isMePayer,
            ),
        ),
        categoryId = AppCategory.FOOD_MARKET.key,
        categories = CategoryTree.appOnly,
        isPending = isPending,
        myBalanceMinor = if (isMePayer) PREVIEW_TOTAL_MINOR - PREVIEW_SHARE_MINOR else -PREVIEW_SHARE_MINOR,
        myPartNote = if (isMePayer) {
            MyPartNote.OwedBy(
                names = listOf("Bruno", "Ana", "Carla", "Diego"),
                eachMinor = PREVIEW_SHARE_MINOR,
                totalMinor = PREVIEW_SHARE_MINOR * 4,
            )
        } else {
            MyPartNote.OwesPayers(listOf("Bruno"))
        },
        splitMode = SplitMode.EQUAL,
        people = people,
        deletion = null,
        isDeleting = false,
        isLoaded = true,
        threads = if (isPending) emptyList() else createPreviewThreads(isReplyingOffline),
        commentDraft = if (isReplyingOffline) "Sem pressa, Diego!" else "",
        replyTarget = if (isReplyingOffline) {
            BillReplyTarget(
                threadId = "c4",
                commentId = "c4",
                name = "Diego",
                isMe = false,
            )
        } else {
            null
        },
        history = if (isPending) createPendingPreviewHistory() else createPreviewHistory(),
        isOnline = !isReplyingOffline,
        isUndoing = false,
        attachments = if (isPending) createPendingPreviewAttachments() else createPreviewAttachments(),
        viewedAttachmentId = null,
        isAddingPhoto = false,
    )
}

// Why: the receipt the bill was read from, then Ana's two photos; previews load no image, so the placeholders show.
private fun createPreviewAttachments(): List<BillAttachmentTile> = listOf(
    createPreviewAttachment(
        "a1",
        photoNumber = null,
        adderName = "Bruno",
    ),
    createPreviewAttachment(
        "a2",
        photoNumber = 1,
        adderName = "Ana",
    ),
    createPreviewAttachment(
        "a3",
        photoNumber = 2,
        adderName = "Ana",
    ),
)

// Why: a photo taken offline, waiting in the queue with its bill.
private fun createPendingPreviewAttachments(): List<BillAttachmentTile> = listOf(
    createPreviewAttachment(
        "a1",
        photoNumber = 1,
        adderName = "Pierre",
        isAddedByMe = true,
        isPending = true,
    ),
)

private fun createPreviewAttachment(
    id: String,
    photoNumber: Int?,
    adderName: String,
    isAddedByMe: Boolean = false,
    isPending: Boolean = false,
): BillAttachmentTile = BillAttachmentTile(
    id = id,
    isReceipt = photoNumber == null,
    localImage = null,
    imageUrl = null,
    width = null,
    height = null,
    photoNumber = photoNumber,
    adderName = adderName,
    isAddedByMe = isAddedByMe,
    addedAt = PREVIEW_SPENT_AT,
    addedDay = if (isPending) RelativeDay.TODAY else RelativeDay.YESTERDAY,
    isPending = isPending,
    canRemove = photoNumber != null,
)

// Why: Carla's thread, where Bruno answered and fixed the split, then Diego through the bill's link.
private fun createPreviewThreads(hasQueuedReply: Boolean): List<BillCommentThread> = listOf(
    BillCommentThread(
        comment = createPreviewComment(
            "c1",
            "Carla",
            "CA",
            AvatarColor.LILAC,
            "A cerveja que ficou comigo foi do Bruno, eu só tomei água.",
        ),
        replies = listOf(
            createPreviewComment(
                "c2",
                "Bruno",
                "BR",
                AvatarColor.GREEN,
                "Verdade, passei para mim. Sua parte ficou R$ 98,00.",
            ),
            createPreviewComment("c3", "Carla", "CA", AvatarColor.LILAC, "Valeu, Bruno!", replyToName = "Bruno"),
        ),
    ),
    BillCommentThread(
        comment = createPreviewComment(
            "c4",
            "Diego",
            "DI",
            AvatarColor.SAND,
            "Pago a minha parte amanhã, Bruno.",
            viaLink = true,
        ),
        replies = if (hasQueuedReply) {
            listOf(
                createPreviewComment(
                    "c5",
                    "Pierre",
                    "P",
                    AvatarColor.PEACH,
                    "Pode ser no Pix.",
                    isMe = true,
                    isPending = true,
                ),
            )
        } else {
            emptyList()
        },
    ),
)

private fun createPreviewComment(
    id: String,
    name: String,
    initials: String,
    color: AvatarColor,
    body: String,
    isMe: Boolean = false,
    viaLink: Boolean = false,
    replyToName: String? = null,
    isPending: Boolean = false,
): BillCommentRow = BillCommentRow(
    id = id,
    authorName = name,
    initials = initials,
    avatarColor = color,
    photoUrl = null,
    isMe = isMe,
    viaLink = viaLink,
    replyToName = replyToName,
    body = body,
    postedAt = PREVIEW_NOW,
    postedDay = RelativeDay.TODAY,
    isPending = isPending,
)

// Why: Ana's category change with Desfazer, Bruno's split, Ana's photos, the link, and how Bruno created it.
private fun createPreviewHistory(): List<BillHistoryDay> = listOf(
    BillHistoryDay(
        dayStartMillis = PREVIEW_NOW,
        relativeDay = RelativeDay.TODAY,
        entries = listOf(
            createPreviewEntry(
                id = "h1",
                kind = BillHistoryKind.EDITED,
                actor = "Ana",
                changes = listOf(BillHistoryChange.Category(AppCategory.FOOD_BAR.key, AppCategory.FOOD_RESTAURANT.key)),
                canUndo = true,
            ),
            createPreviewEntry(
                id = "h2",
                kind = BillHistoryKind.EDITED,
                actor = "Bruno",
                changes = listOf(
                    BillHistoryChange.Split(
                        beforeMode = SplitMode.EQUAL,
                        afterMode = SplitMode.AMOUNT,
                        parts = listOf(
                            BillHistoryPart(
                                "Carla",
                                isMe = false,
                                beforeMinor = 11_200L,
                                afterMinor = 9_800L,
                            ),
                            BillHistoryPart(
                                "Bruno",
                                isMe = false,
                                beforeMinor = 13_200L,
                                afterMinor = 14_600L,
                            ),
                            BillHistoryPart(
                                "Elisa",
                                isMe = false,
                                beforeMinor = 8_400L,
                                afterMinor = null,
                            ),
                        ),
                    ),
                    BillHistoryChange.Amount(61_200L, PREVIEW_TOTAL_MINOR),
                ),
            ),
        ),
    ),
    BillHistoryDay(
        dayStartMillis = PREVIEW_SPENT_AT,
        relativeDay = RelativeDay.YESTERDAY,
        entries = listOf(
            createPreviewEntry("h3", BillHistoryKind.ATTACHMENTS_ADDED, "Ana", attachmentCount = 2),
            createPreviewEntry("h4", BillHistoryKind.LINK_CHANGED, "Bruno", linkVisibility = BillLinkVisibility.ANYONE),
            createPreviewEntry(
                "h5",
                BillHistoryKind.CREATED,
                "Bruno",
                splitMode = SplitMode.AMOUNT,
                sharesCount = 6,
            ),
        ),
    ),
)

private fun createPendingPreviewHistory(): List<BillHistoryDay> = listOf(
    BillHistoryDay(
        dayStartMillis = PREVIEW_NOW,
        relativeDay = RelativeDay.TODAY,
        entries = listOf(
            createPreviewEntry(
                id = "h1",
                kind = BillHistoryKind.CREATED,
                actor = "Pierre",
                isMe = true,
                splitMode = SplitMode.EQUAL,
                sharesCount = 5,
                isPending = true,
            ),
        ),
    ),
)

private fun createPreviewEntry(
    id: String,
    kind: BillHistoryKind,
    actor: String,
    isMe: Boolean = false,
    changes: List<BillHistoryChange> = emptyList(),
    splitMode: SplitMode? = null,
    sharesCount: Int? = null,
    attachmentCount: Int = 0,
    linkVisibility: BillLinkVisibility? = null,
    isPending: Boolean = false,
    canUndo: Boolean = false,
): BillHistoryEntry = BillHistoryEntry(
    id = id,
    kind = kind,
    actorName = actor,
    actorInitials = actor.take(2).uppercase(),
    actorColor = if (isMe) AvatarColor.PEACH else AvatarColor.GREEN,
    actorPhotoUrl = null,
    actorIsMe = isMe,
    occurredAt = PREVIEW_NOW,
    changes = changes,
    amountMinor = PREVIEW_TOTAL_MINOR.takeIf { kind == BillHistoryKind.CREATED },
    splitMode = splitMode,
    sharesCount = sharesCount,
    attachmentCount = attachmentCount,
    isReceipt = false,
    linkVisibility = linkVisibility,
    isPending = isPending,
    canUndo = canUndo,
)

private fun createPreviewRow(
    id: String,
    name: String,
    initials: String,
    color: AvatarColor,
    isMe: Boolean = false,
    hasJoined: Boolean = true,
): BillShareRow = BillShareRow(
    memberId = id,
    name = name,
    initials = initials,
    avatarColor = color,
    photoUrl = null,
    isMe = isMe,
    hasJoined = hasJoined,
    paidMinor = null,
    shareMinor = null,
)
