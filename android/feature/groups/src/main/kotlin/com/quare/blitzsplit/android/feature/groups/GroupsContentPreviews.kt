package com.quare.blitzsplit.android.feature.groups

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.coverFoldLayoutOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.core.model.GroupGridSize
import com.quare.blitzsplit.core.model.GroupsDisplay
import com.quare.blitzsplit.core.model.GroupsLayout
import com.quare.blitzsplit.feature.groups.presentation.model.ActivityAge
import com.quare.blitzsplit.feature.groups.presentation.model.BalanceTotal
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearch
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSearchResults
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSummary
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiState
import com.quare.blitzsplit.feature.groups.presentation.model.LastActivity
import com.quare.blitzsplit.feature.groups.presentation.model.LatestBill
import com.quare.blitzsplit.feature.groups.presentation.model.LatestBillAuthor
import com.quare.blitzsplit.feature.groups.presentation.model.MemberFace
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchBillPayer
import com.quare.blitzsplit.feature.groups.presentation.model.SearchGroupHit
import com.quare.blitzsplit.feature.groups.presentation.model.SearchScope
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge
import com.quare.blitzsplit.feature.groups.presentation.model.TextMatch

@DevicePreviews
@Composable
private fun GroupsOfflinePreview() {
    BlitzSplitTheme {
        GroupsContent(
            state = GroupsUiState(
                displayName = "Pierre",
                avatarInitial = "P",
                avatarColor = AvatarColor.BLUE,
                photoUrl = null,
                accountProvider = null,
                syncBadge = SyncBadge.OFFLINE,
                pendingChangeCount = 0,
                showsRecoveryCodePrompt = false,
                groups = emptyList(),
                hasLoadedGroups = true,
                summary = GroupsSummary(totals = emptyList()),
                showsSettledGroups = false,
                filterQuery = "",
                isRefreshing = false,
                deletedGroupNotice = null,
                exit = GroupExitState.idle,
                exitGroupId = null,
                layout = GroupsLayout.initial,
                recordedPayment = null,
                search = GroupsSearch.closed,
                selectedGroupId = null,
                latestBill = null,
                activityUnreadCount = 3,
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupsListPreview() {
    BlitzSplitTheme {
        GroupsContent(
            state = buildPreviewState(GroupsLayout.initial),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupsGridPreview() {
    BlitzSplitTheme {
        GroupsContent(
            state = buildPreviewState(GroupsLayout(GroupsDisplay.GRID, GroupGridSize.STANDARD)),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupsCompactGridPreview() {
    BlitzSplitTheme {
        GroupsContent(
            state = buildPreviewState(GroupsLayout(GroupsDisplay.GRID, GroupGridSize.COMPACT)),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupsSearchPreview() {
    val churrasco = buildPreviewRow(
        id = "4",
        name = "Churrasco de sábado",
        color = AvatarColor.BLUE,
        kind = GroupBalanceKind.TO_RECEIVE,
        receivesMinor = 9_750L,
        owesMinor = 0L,
    )
    val search = GroupsSearch(
        isOpen = true,
        query = "chur",
        scope = SearchScope.ALL,
        results = GroupsSearchResults(
            groups = listOf(
                SearchGroupHit(
                    group = churrasco,
                    nameMatch = TextMatch(0, 4),
                    billDescription = null,
                    billMatch = null,
                ),
            ),
            people = emptyList(),
            bills = listOf(
                SearchBillHit(
                    id = "b",
                    groupId = "4",
                    groupName = churrasco.name,
                    description = "Carne e carvão do churrasco",
                    descriptionMatch = TextMatch(18, 22),
                    totalMinor = 48_600L,
                    currency = GroupCurrency.BRL,
                    payers = listOf(SearchBillPayer("Bruno", isMe = false)),
                    spentAt = 0L,
                ),
            ),
        ),
    )
    BlitzSplitTheme {
        GroupsContent(
            state = buildPreviewState(GroupsLayout.initial, search),
            onEvent = {},
        )
    }
}

// Why: a Flip's cover screen (360×374), which a preview only knows through its fold layout.
@Preview(
    name = "Flip cover",
    device = "spec:width=360dp,height=374dp",
)
@Composable
private fun GroupsCoverPreview() {
    CompositionLocalProvider(LocalFoldLayout provides coverFoldLayoutOf(360.dp, 374.dp)) {
        BlitzSplitTheme {
            GroupsContent(
                state = buildPreviewState(GroupsLayout.initial).copy(
                    latestBill = LatestBill(
                        description = "Uber aeroporto",
                        amountMinor = 8760L,
                        currency = GroupCurrency.BRL,
                        groupName = "Viagem Floripa",
                        savedBy = LatestBillAuthor.Member("Ana"),
                    ),
                ),
                onEvent = {},
            )
        }
    }
}

private fun buildPreviewState(
    layout: GroupsLayout,
    search: GroupsSearch = GroupsSearch.closed,
): GroupsUiState {
    val rows = listOf(
        buildPreviewRow(
            id = "1",
            name = "Viagem Floripa",
            color = AvatarColor.PEACH,
            kind = GroupBalanceKind.TO_RECEIVE,
            receivesMinor = 21_490L,
            owesMinor = 0L,
        ),
        buildPreviewRow(
            id = "2",
            name = "Apê 302",
            color = AvatarColor.BLUE,
            kind = GroupBalanceKind.YOU_OWE,
            receivesMinor = 12_000L,
            owesMinor = 21_800L,
        ),
        buildPreviewRow(
            id = "3",
            name = "Aniversário da Ana",
            color = AvatarColor.LILAC,
            kind = GroupBalanceKind.EVERYONE_SETTLED,
            receivesMinor = 0L,
            owesMinor = 0L,
        ),
    )
    return GroupsUiState(
        displayName = "Pierre",
        avatarInitial = "P",
        avatarColor = AvatarColor.BLUE,
        photoUrl = null,
        accountProvider = null,
        syncBadge = SyncBadge.OFFLINE,
        pendingChangeCount = 1,
        showsRecoveryCodePrompt = false,
        groups = rows,
        hasLoadedGroups = true,
        summary = GroupsSummary(
            totals = listOf(
                BalanceTotal(
                    currency = GroupCurrency.BRL,
                    netMinor = 11_690L,
                    toReceiveMinor = 21_490L,
                    toPayMinor = 9_800L,
                    toReceiveGroupCount = 1,
                    toPayGroupCount = 1,
                ),
            ),
        ),
        showsSettledGroups = true,
        filterQuery = "",
        isRefreshing = false,
        deletedGroupNotice = null,
        exit = GroupExitState.idle,
        exitGroupId = null,
        layout = layout,
        recordedPayment = null,
        search = search,
        selectedGroupId = null,
        latestBill = null,
        activityUnreadCount = 3,
    )
}

internal fun buildPreviewRow(
    id: String,
    name: String,
    color: AvatarColor,
    kind: GroupBalanceKind,
    receivesMinor: Long,
    owesMinor: Long,
): GroupRow = GroupRow(
    id = id,
    name = name,
    initials = name.take(2).uppercase(),
    color = color,
    faces = listOf(
        MemberFace("P", AvatarColor.BLUE),
        MemberFace("A", AvatarColor.GREEN),
        MemberFace("B", AvatarColor.PEACH),
    ),
    extraMemberCount = 3,
    memberCount = 6,
    memberNames = listOf("Pierre", "Ana", "Bruno"),
    isPending = false,
    pendingBillDescription = null,
    currency = GroupCurrency.BRL,
    balanceKind = kind,
    netMinor = receivesMinor - owesMinor,
    receivesMinor = receivesMinor,
    owesMinor = owesMinor,
    lastActivity = LastActivity(
        epochMillis = 0L,
        age = ActivityAge.DAYS,
        count = 3,
    ),
)
