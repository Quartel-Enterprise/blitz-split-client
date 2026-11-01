package com.quare.blitzsplit.android.feature.groups

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.tabletopFoldLayoutOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.PeopleTotal
import com.quare.blitzsplit.core.expenses.domain.model.RelativeDay
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBill
import com.quare.blitzsplit.feature.groups.presentation.model.HomeBillDay
import com.quare.blitzsplit.feature.groups.presentation.model.HomeGroup
import com.quare.blitzsplit.feature.groups.presentation.model.HomePerson
import com.quare.blitzsplit.feature.groups.presentation.model.HomeSummary
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillPayer
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge

@DevicePreviews
@Composable
private fun HomePreview() {
    BlitzSplitTheme {
        HomeContent(
            state = buildHomePreviewState(),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun HomeOfflineWithRecoveryCardPreview() {
    BlitzSplitTheme {
        HomeContent(
            state = buildHomePreviewState().copy(
                syncBadge = SyncBadge.OFFLINE,
                pendingChangeCount = 2,
                isOnline = false,
                showsRecoveryCodeCard = true,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun HomeSettledPreview() {
    BlitzSplitTheme {
        HomeContent(
            state = buildHomePreviewState().copy(
                people = emptyList(),
                summary = HomeSummary(
                    totals = listOf(
                        PeopleTotal(
                            currency = GroupCurrency.BRL,
                            netMinor = 0L,
                            toReceiveMinor = 0L,
                            toPayMinor = 0L,
                            toReceiveCount = 0,
                            toPayCount = 0,
                        ),
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun HomeFirstDownloadPreview() {
    BlitzSplitTheme {
        HomeContent(
            state = buildHomePreviewState().copy(
                isFirstDownload = true,
                syncBadge = SyncBadge.SYNCING,
                people = emptyList(),
                billDays = emptyList(),
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun HomeFirstUsePreview() {
    BlitzSplitTheme {
        HomeContent(
            state = buildHomePreviewState().copy(
                groupCount = 0,
                people = emptyList(),
                groups = emptyList(),
                billDays = emptyList(),
            ),
            onEvent = {},
        )
    }
}

@Preview(
    widthDp = 841,
    heightDp = 673,
)
@Composable
private fun HomeTabletopPreview() {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme {
            HomeContent(
                state = buildHomePreviewState(),
                onEvent = {},
            )
        }
    }
}

// Why: a Flip's cover screen.
@Preview(
    widthDp = 360,
    heightDp = 374,
)
@Composable
private fun HomeCoverPreview() {
    BlitzSplitTheme {
        HomeContent(
            state = buildHomePreviewState(),
            onEvent = {},
        )
    }
}

private fun buildHomePreviewState(): HomeUiState = HomeUiState(
    displayName = "Pierre",
    avatarInitial = "P",
    avatarColor = AvatarColor.BLUE,
    photoUrl = null,
    accountProvider = null,
    syncBadge = null,
    pendingChangeCount = 0,
    isFirstDownload = false,
    isOnline = true,
    isLoaded = true,
    groupCount = 4,
    people = listOf(
        buildPreviewPerson(
            "Bruno",
            AvatarColor.PEACH,
            netMinor = -9_800L,
            isGuest = false,
        ),
        buildPreviewPerson(
            "Ana",
            AvatarColor.GREEN,
            netMinor = 16_230L,
            isGuest = false,
        ),
        buildPreviewPerson(
            "Carla",
            AvatarColor.LILAC,
            netMinor = 3_290L,
            isGuest = true,
        ),
    ),
    summary = HomeSummary(
        totals = listOf(
            PeopleTotal(
                currency = GroupCurrency.BRL,
                netMinor = 9_720L,
                toReceiveMinor = 19_520L,
                toPayMinor = 9_800L,
                toReceiveCount = 2,
                toPayCount = 1,
            ),
        ),
    ),
    groups = listOf(
        HomeGroup(
            row = buildPreviewRow(
                id = "1",
                name = "Viagem Floripa",
                color = AvatarColor.PEACH,
                kind = GroupBalanceKind.TO_RECEIVE,
                receivesMinor = 21_490L,
                owesMinor = 0L,
            ),
            isPinned = true,
        ),
        HomeGroup(
            row = buildPreviewRow(
                id = "2",
                name = "Apê 302",
                color = AvatarColor.BLUE,
                kind = GroupBalanceKind.YOU_OWE,
                receivesMinor = 0L,
                owesMinor = 9_800L,
            ),
            isPinned = false,
        ),
    ),
    billDays = listOf(
        HomeBillDay(
            dayStartMillis = 0L,
            relativeDay = RelativeDay.TODAY,
            bills = listOf(
                HomeBill(
                    id = "b1",
                    groupId = "1",
                    groupName = "Viagem Floripa",
                    description = "Mercado",
                    totalMinor = 31_200L,
                    currency = GroupCurrency.BRL,
                    payers = listOf(SearchBillPayer("Pierre", isMe = true)),
                    myBalanceMinor = 24_960L,
                    isPending = true,
                ),
            ),
        ),
        HomeBillDay(
            dayStartMillis = 0L,
            relativeDay = RelativeDay.YESTERDAY,
            bills = listOf(
                HomeBill(
                    id = "b2",
                    groupId = "2",
                    groupName = "Apê 302",
                    description = "Conta de luz",
                    totalMinor = 19_600L,
                    currency = GroupCurrency.BRL,
                    payers = listOf(SearchBillPayer("Bruno", isMe = false)),
                    myBalanceMinor = -9_800L,
                    isPending = false,
                ),
            ),
        ),
    ),
    showsRecoveryCodeCard = false,
    showsRecoveryCodePrompt = false,
    recordedPayment = null,
    deletedGroupNotice = null,
    isRefreshing = false,
)

private fun buildPreviewPerson(
    name: String,
    color: AvatarColor,
    netMinor: Long,
    isGuest: Boolean,
): HomePerson = HomePerson(
    key = name,
    name = name,
    initials = name.take(1),
    color = color,
    currency = GroupCurrency.BRL,
    netMinor = netMinor,
    isGuest = isGuest,
)
