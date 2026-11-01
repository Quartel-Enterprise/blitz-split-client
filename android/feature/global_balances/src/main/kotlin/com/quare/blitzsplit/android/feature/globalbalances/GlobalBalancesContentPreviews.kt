package com.quare.blitzsplit.android.feature.globalbalances

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.PersonGroupBalance
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.BalancesFilter
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.globalbalances.presentation.model.GlobalBalancesUiState
import com.quare.blitzsplit.feature.globalbalances.presentation.model.PersonRow

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
            createPreviewPart("Viagem Floripa", "VF", -11_800L),
            createPreviewPart("Futebol de quinta", "FQ", -2_800L),
            createPreviewPart("Churrasco de sábado", "CS", 2_000L),
        ),
    ),
    createPreviewRow("Ana", 16_230L, listOf(createPreviewPart("Viagem Floripa", "VF", 16_230L))),
    createPreviewRow("Diego", 4_500L, listOf(createPreviewPart("Churrasco de sábado", "CS", 4_500L)), isGuest = true),
    createPreviewRow(
        "Carla",
        3_290L,
        listOf(
            createPreviewPart("Viagem Floripa", "VF", 9_840L),
            createPreviewPart("Apê 302", "A3", -9_800L),
            createPreviewPart("Churrasco de sábado", "CS", 3_250L),
        ),
        isExpanded = true,
    ),
)

private fun createPreviewPart(
    name: String,
    initials: String,
    amountMinor: Long,
): PersonGroupBalance = PersonGroupBalance(
    groupId = name,
    groupName = name,
    groupInitials = initials,
    groupColor = AvatarColor.PEACH,
    memberId = "member",
    meMemberId = "me",
    amountMinor = amountMinor,
)

private fun createPreviewRow(
    name: String,
    netMinor: Long,
    groups: List<PersonGroupBalance>,
    isGuest: Boolean = false,
    isExpanded: Boolean = false,
): PersonRow = PersonRow(
    id = name,
    name = name,
    initials = name.take(2).uppercase(),
    color = AvatarColor.LILAC,
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
    people = people,
    totals = listOf(previewTotal),
    filter = filter,
    pixKey = null,
    charge = null,
    isOnline = isOnline,
    isLoaded = true,
    recordedPayment = null,
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
private fun GlobalBalancesSettledPreview() {
    BlitzSplitTheme {
        GlobalBalancesContent(
            state = createPreviewState(emptyList()),
            onEvent = {},
        )
    }
}
