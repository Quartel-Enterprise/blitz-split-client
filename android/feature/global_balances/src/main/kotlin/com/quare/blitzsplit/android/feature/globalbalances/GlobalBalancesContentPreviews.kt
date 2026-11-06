package com.quare.blitzsplit.android.feature.globalbalances

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.previewInitialsOf
import com.quare.blitzsplit.android.designsystem.layout.tabletopFoldLayoutOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.PersonGroupBalance
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.PaymentKey
import com.quare.blitzsplit.core.model.PaymentKeyType
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeDraft
import com.quare.blitzsplit.feature.globalbalances.presentation.model.ChargeReroute
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiState
import com.quare.blitzsplit.feature.globalbalances.presentation.model.PersonRow
import software.quare.loadable.Loadable
import software.quare.loadable.toLoaded

private val previewTotal = PeopleTotal(
    currency = GroupCurrency.BRL,
    netMinor = 18_640L,
    toReceiveMinor = 31_240L,
    toPayMinor = 12_600L,
    toReceiveCount = 4,
    toPayCount = 1,
)
private val previewPeople = listOf(
    createPreviewRow(
        "Bruno",
        -12_600L,
        listOf(
            createPreviewPart("Viagem Floripa", previewInitialsOf("Viagem Floripa"), -11_800L),
            createPreviewPart("Futebol de quinta", "FQ", -2_800L),
            createPreviewPart("Churrasco de sábado", "CS", 2_000L),
        ),
    ),
    createPreviewRow(
        "Ana",
        16_230L,
        listOf(createPreviewPart("Viagem Floripa", previewInitialsOf("Viagem Floripa"), 16_230L)),
    ),
    createPreviewRow("Diego", 4_500L, listOf(createPreviewPart("Churrasco de sábado", "CS", 4_500L)), isGuest = true),
    createPreviewRow(
        "Carla",
        3_290L,
        listOf(
            createPreviewPart(
                name = "Viagem Floripa",
                initials = previewInitialsOf("Viagem Floripa"),
                amountMinor = 9_840L,
                insteadOf = listOf("Elisa"),
            ),
            createPreviewPart("Apê 302", "A3", -9_800L),
            createPreviewPart("Churrasco de sábado", "CS", 3_250L),
        ),
        isExpanded = true,
    ),
)
private val previewCharge = ChargeDraft(
    personId = "Carla",
    name = "Carla",
    initials = previewInitialsOf("Carla"),
    color = AvatarColor.LILAC,
    photoUrl = null,
    currency = GroupCurrency.BRL,
    amountMinor = 3_290L,
    groupNames = listOf("Viagem Floripa", "Apê 302", "Churrasco de sábado"),
    isGuest = false,
    guestBills = emptyList(),
    guestPaidMinor = null,
    hasPixKey = true,
    pixKeyChoice = null,
    editedMessage = null,
    reroutes = listOf(
        ChargeReroute(
            groupName = "Viagem Floripa",
            insteadOfNames = listOf("Elisa"),
        ),
    ),
)
private val previewPixKey = PaymentKey(
    id = "pix",
    type = PaymentKeyType.EMAIL,
    value = "pierre@email.com",
    isDefault = true,
    addedAt = 0L,
)

private fun createPreviewPart(
    name: String,
    initials: String,
    amountMinor: Long,
    // Why: a group whose "Simplificar dívidas" made this pair, instead of paying whom (13.6).
    insteadOf: List<String> = emptyList(),
): PersonGroupBalance = PersonGroupBalance(
    groupId = name,
    groupName = name,
    groupInitials = initials,
    groupColor = AvatarColor.PEACH,
    memberId = "member",
    meMemberId = "me",
    amountMinor = amountMinor,
    isSimplified = insteadOf.isNotEmpty(),
    insteadOfNames = insteadOf,
)

private fun createPreviewRow(
    name: String,
    netMinor: Long,
    groups: List<PersonGroupBalance>,
    isGuest: Boolean = false,
    isExpanded: Boolean = false,
): PersonRow = PersonRow(
    id = name,
    personKey = name,
    name = name,
    initials = previewInitialsOf(name),
    color = AvatarColor.LILAC,
    photoUrl = null,
    currency = GroupCurrency.BRL,
    netMinor = netMinor,
    groups = groups,
    isGuest = isGuest,
    isExpanded = isExpanded,
)

private fun createPreviewState(
    people: List<PersonRow>,
    filter: BalancesFilter = BalancesFilter.ALL,
    isOnline: Boolean = true,
): GlobalBalancesUiState = GlobalBalancesUiState(
    displayName = "Pierre",
    avatarInitial = "P",
    avatarColor = AvatarColor.BLUE,
    photoUrl = null,
    accountProvider = null,
    people = people.toLoaded(),
    totals = listOf(previewTotal).toLoaded(),
    isWaitingForInternet = false,
    filter = filter,
    isOnline = isOnline,
    recordedPayment = null,
    pickedPersonId = null,
    canSearch = true,
    showsSplitBillButton = true,
)

@DevicePreviews
@Composable
private fun GlobalBalancesPreview() {
    BlitzSplitTheme {
        GlobalBalancesContent(
            state = createPreviewState(previewPeople),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GlobalBalancesOfflinePreview() {
    BlitzSplitTheme {
        GlobalBalancesContent(
            state = createPreviewState(previewPeople, isOnline = false),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GlobalBalancesLoadingPreview() {
    BlitzSplitTheme {
        GlobalBalancesContent(
            state = createPreviewState(emptyList(), isOnline = false).copy(
                people = Loadable.Loading,
                totals = Loadable.Loading,
                isWaitingForInternet = true,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GlobalBalancesSettledPreview() {
    BlitzSplitTheme {
        GlobalBalancesContent(
            state = createPreviewState(emptyList()),
            onEvent = {},
        )
    }
}

// Why: the board draws no tabletop for page 8, so Saldos stays as on the book (ChargeDialogTabletopPreview shows Cobrar).
@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun GlobalBalancesTabletopPreview() {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme {
            GlobalBalancesContent(
                state = createPreviewState(previewPeople),
                onEvent = {},
            )
        }
    }
}

@DevicePreviews
@Composable
private fun ChargeSheetPreview() {
    BlitzSplitTheme {
        ChargeContent(
            charge = previewCharge,
            pixKey = previewPixKey,
            isDialog = false,
            onEvent = {},
        )
    }
}

// Why: a tabletop lying down draws Cobrar as a dialog over the hinge, the tightest pane it gets, Pix key included.
@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun ChargeDialogTabletopPreview() {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme {
            ChargeContent(
                charge = previewCharge,
                pixKey = previewPixKey,
                isDialog = true,
                onEvent = {},
            )
        }
    }
}
