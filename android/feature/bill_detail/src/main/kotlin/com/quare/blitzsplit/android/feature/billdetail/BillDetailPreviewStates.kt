package com.quare.blitzsplit.android.feature.billdetail

import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SplitMode
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillDetailUiState
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillPayer
import com.quare.blitzsplit.feature.billdetail.presentation.model.BillShareRow
import com.quare.blitzsplit.feature.billdetail.presentation.model.MyPartNote

private const val PREVIEW_SPENT_AT = 1_761_683_400_000L
private const val PREVIEW_TOTAL_MINOR = 64_200L
private const val PREVIEW_SHARE_MINOR = 12_840L

internal fun createPreviewBillDetailState(
    isMePayer: Boolean = false,
    isPending: Boolean = false,
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
        payer = BillPayer(
            name = if (isMePayer) "Pierre" else "Bruno",
            initials = if (isMePayer) "P" else "BR",
            avatarColor = if (isMePayer) AvatarColor.PEACH else AvatarColor.GREEN,
            isMe = isMePayer,
        ),
        isPending = isPending,
        myBalanceMinor = if (isMePayer) PREVIEW_TOTAL_MINOR - PREVIEW_SHARE_MINOR else -PREVIEW_SHARE_MINOR,
        myPartNote = if (isMePayer) {
            MyPartNote.OwedBy(
                names = listOf("Bruno", "Ana", "Carla", "Diego"),
                eachMinor = PREVIEW_SHARE_MINOR,
                totalMinor = PREVIEW_SHARE_MINOR * 4,
            )
        } else {
            MyPartNote.OwesPayer("Bruno")
        },
        splitMode = SplitMode.EQUAL,
        people = people,
        deletion = null,
        isDeleting = false,
        isLoaded = true,
    )
}

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
    isMe = isMe,
    hasJoined = hasJoined,
    paidMinor = null,
    shareMinor = null,
)
