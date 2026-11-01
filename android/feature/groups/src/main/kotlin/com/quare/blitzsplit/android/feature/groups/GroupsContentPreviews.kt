package com.quare.blitzsplit.android.feature.groups

import androidx.compose.runtime.Composable
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.expenses.domain.model.GroupExitState
import com.quare.blitzsplit.core.model.AvatarColor
import com.quare.blitzsplit.core.model.GroupCurrency
import com.quare.blitzsplit.feature.groups.presentation.model.ActivityAge
import com.quare.blitzsplit.feature.groups.presentation.model.BalanceTotal
import com.quare.blitzsplit.feature.groups.presentation.model.GroupBalanceKind
import com.quare.blitzsplit.feature.groups.presentation.model.GroupRow
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsSummary
import com.quare.blitzsplit.feature.groups.presentation.model.GroupsUiState
import com.quare.blitzsplit.feature.groups.presentation.model.LastActivity
import com.quare.blitzsplit.feature.groups.presentation.model.MemberFace
import com.quare.blitzsplit.feature.groups.presentation.model.SyncBadge

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
            ),
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupsListPreview() {
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
    BlitzSplitTheme {
        GroupsContent(
            state = GroupsUiState(
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
            ),
            onEvent = {},
        )
    }
}

private fun buildPreviewRow(
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
