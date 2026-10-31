package com.quare.blitzsplit.android.feature.groupdetail

import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groupdetail.presentation.model.BalancesUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DebtKind
import com.quare.blitzsplit.feature.groupdetail.presentation.model.DebtRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRowPayer
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupFace
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow

private const val PREVIEW_TODAY_MILLIS = 1_761_609_600_000L
private const val PREVIEW_HOUR_MILLIS = 3_600_000L
private const val PREVIEW_DAY_MILLIS = 86_400_000L
private val previewFaces = listOf(
    GroupFace("PV", AvatarColor.BLUE),
    GroupFace("AN", AvatarColor.GREEN),
    GroupFace("BR", AvatarColor.PEACH),
    GroupFace("CA", AvatarColor.LILAC),
)

internal fun createPreviewHeader(hasBills: Boolean = true): GroupHeader = GroupHeader(
    groupName = "Viagem Floripa",
    groupInitials = "VF",
    groupColor = AvatarColor.PEACH,
    currency = GroupCurrency.BRL,
    faces = previewFaces,
    memberCount = 6,
    notJoinedCount = 1,
    totalSpentMinor = if (hasBills) 241_860L else 0L,
    myBalance = if (hasBills) {
        MemberBalance(
            receivesMinor = 33_290L,
            owesMinor = 11_800L,
        )
    } else {
        null
    },
    isEveryoneSettled = false,
)

internal fun createPreviewGroupDetailState(days: List<ExpenseDay>): GroupDetailUiState = GroupDetailUiState(
    header = createPreviewHeader(hasBills = days.isNotEmpty()),
    days = days,
    paymentToDelete = null,
    recordedPayment = null,
    showsBillDeletedNotice = false,
    isLoaded = true,
)

internal fun createPreviewDays(): List<ExpenseDay> = listOf(
    ExpenseDay(
        dayStartMillis = PREVIEW_TODAY_MILLIS,
        relativeDay = RelativeDay.TODAY,
        entries = listOf(
            PaymentRow(
                id = "p1",
                paidAt = PREVIEW_TODAY_MILLIS + 12 * PREVIEW_HOUR_MILLIS,
                fromName = "Ana",
                fromIsMe = false,
                toName = "Pierre",
                toIsMe = true,
                amountMinor = 5_000L,
                method = SettlementMethod.PIX,
                isPending = true,
            ),
            ExpenseRow(
                id = "1",
                description = "Mercado",
                spentAt = PREVIEW_TODAY_MILLIS + 9 * PREVIEW_HOUR_MILLIS,
                payers = listOf(
                    ExpenseRowPayer(
                        name = "Pierre",
                        isMe = true,
                    ),
                ),
                totalMinor = 31_200L,
                myBalanceMinor = 24_960L,
                isPending = false,
            ),
        ),
    ),
    ExpenseDay(
        dayStartMillis = PREVIEW_TODAY_MILLIS - 4 * PREVIEW_DAY_MILLIS,
        relativeDay = RelativeDay.OTHER,
        entries = listOf(
            ExpenseRow(
                id = "3",
                description = "Jantar no Ostradamus",
                spentAt = PREVIEW_TODAY_MILLIS - 4 * PREVIEW_DAY_MILLIS + 21 * PREVIEW_HOUR_MILLIS,
                payers = listOf(
                    ExpenseRowPayer(
                        name = "Bruno",
                        isMe = false,
                    ),
                    ExpenseRowPayer(
                        name = "Ana",
                        isMe = false,
                    ),
                ),
                totalMinor = 64_200L,
                myBalanceMinor = -11_800L,
                isPending = false,
            ),
            ExpenseRow(
                id = "4",
                description = "Passeio de barco",
                spentAt = PREVIEW_TODAY_MILLIS - 4 * PREVIEW_DAY_MILLIS + 15 * PREVIEW_HOUR_MILLIS,
                payers = listOf(
                    ExpenseRowPayer(
                        name = null,
                        isMe = false,
                    ),
                ),
                totalMinor = 72_000L,
                myBalanceMinor = null,
                isPending = false,
            ),
        ),
    ),
)

internal fun createPreviewBalancesState(): BalancesUiState = BalancesUiState(
    header = createPreviewHeader(),
    myDebts = listOf(
        DebtRow(
            fromMemberId = "me",
            toMemberId = "bruno",
            fromName = "Pierre",
            fromFace = previewFaces[0],
            toName = "Bruno",
            toFace = previewFaces[2],
            amountMinor = 11_800L,
            kind = DebtKind.YOU_OWE,
        ),
        DebtRow(
            fromMemberId = "ana",
            toMemberId = "me",
            fromName = "Ana",
            fromFace = previewFaces[1],
            toName = "Pierre",
            toFace = previewFaces[0],
            amountMinor = 16_230L,
            kind = DebtKind.OWES_YOU,
        ),
    ),
    otherDebts = listOf(
        DebtRow(
            fromMemberId = "diego",
            toMemberId = "bruno",
            fromName = null,
            fromFace = null,
            toName = "Bruno",
            toFace = previewFaces[2],
            amountMinor = 6_400L,
            kind = DebtKind.BETWEEN_OTHERS,
        ),
    ),
    hasBills = true,
    recordedPayment = null,
    isLoaded = true,
)
