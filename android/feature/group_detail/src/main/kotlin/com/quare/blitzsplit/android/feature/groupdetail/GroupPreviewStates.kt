package com.quare.blitzsplit.android.feature.groupdetail

import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.core.expenses.domain.model.AppCategory
import com.quare.blitzsplit.core.expenses.domain.model.CategoryTree
import com.quare.blitzsplit.core.expenses.domain.model.DebtKind
import com.quare.blitzsplit.core.expenses.domain.model.DebtRow
import com.quare.blitzsplit.core.expenses.domain.model.DebtSimplificationSummary
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.expenses.domain.model.GroupFace
import com.quare.blitzsplit.core.expenses.domain.model.MemberBalance
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.expenses.domain.model.SettlementMethod
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseDay
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRow
import com.quare.blitzsplit.feature.groupdetail.presentation.model.ExpenseRowPayer
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader
import com.quare.blitzsplit.feature.groupdetail.presentation.model.PaymentRow

private const val PREVIEW_TODAY_MILLIS = 1_761_609_600_000L
private const val PREVIEW_HOUR_MILLIS = 3_600_000L
private const val PREVIEW_DAY_MILLIS = 86_400_000L
private val previewFaces = listOf(
    GroupFace("P", AvatarColor.BLUE, photoUrl = null),
    GroupFace("A", AvatarColor.GREEN, photoUrl = null),
    GroupFace("B", AvatarColor.PEACH, photoUrl = null),
    GroupFace("C", AvatarColor.LILAC, photoUrl = null),
)

internal fun createPreviewHeader(hasBills: Boolean = true): GroupHeader = GroupHeader(
    groupName = "Viagem Floripa",
    groupInitials = previewInitialsOf("Viagem Floripa"),
    groupColor = AvatarColor.PEACH,
    groupPhotoUrl = null,
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
    isPinned = true,
)

internal val previewGroupExit = GroupExitState(
    isOnline = true,
    dialog = null,
    isRunning = false,
    deletedNotice = null,
)

internal fun createPreviewGroupDetailState(days: List<ExpenseDay>): GroupDetailUiState = GroupDetailUiState(
    header = createPreviewHeader(hasBills = days.isNotEmpty()),
    tab = GroupTab.EXPENSES,
    days = days,
    paymentToView = null,
    paymentToDelete = null,
    billToDelete = null,
    isDeletingBill = false,
    myDebts = emptyList(),
    otherDebts = emptyList(),
    hasBills = days.isNotEmpty(),
    recordedPayment = null,
    showsBillDeletedNotice = false,
    exit = previewGroupExit,
    isLoaded = true,
    awaitsFullSync = false,
    isWaitingForInternet = false,
    teach = null,
    homeTab = HomeTab.GROUPS,
    joinedAs = null,
    isAccountUnprotected = false,
    simplification = null,
    simplifyConflict = null,
)

internal fun createPreviewDays(): List<ExpenseDay> = listOf(
    ExpenseDay(
        dayStartMillis = PREVIEW_TODAY_MILLIS,
        relativeDay = RelativeDay.TODAY,
        entries = listOf(
            PaymentRow(
                id = "p1",
                paidAt = PREVIEW_TODAY_MILLIS + 12 * PREVIEW_HOUR_MILLIS,
                paidDay = RelativeDay.TODAY,
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
                category = CategoryTree.appOnly.resolve(AppCategory.FOOD_RESTAURANT.key),
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
                category = CategoryTree.appOnly.resolve(AppCategory.FOOD_RESTAURANT.key),
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
                category = CategoryTree.appOnly.resolve(AppCategory.FOOD_RESTAURANT.key),
                isPending = false,
            ),
        ),
    ),
)

internal fun createPreviewBalancesState(): GroupDetailUiState = createPreviewGroupDetailState(
    days = createPreviewDays(),
).copy(
    tab = GroupTab.BALANCES,
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
            isSimplified = false,
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
            isSimplified = false,
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
            isSimplified = false,
        ),
    ),
)

// Why: 13.4, "Simplificar dívidas" on: the card over the pairs and the icon on the pair no bill made.
internal fun createPreviewSimplifiedBalancesState(): GroupDetailUiState {
    val state = createPreviewBalancesState()
    return state.copy(
        myDebts = state.myDebts.mapIndexed { index, debt -> debt.copy(isSimplified = index == 1) },
        simplification = DebtSimplificationSummary(
            paymentCount = 5,
            unsimplifiedPaymentCount = 7,
        ),
    )
}
