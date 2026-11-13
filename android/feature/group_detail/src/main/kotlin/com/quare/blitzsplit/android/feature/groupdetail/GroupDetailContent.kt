package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.splitBillIcon
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.LocalInDetailPane
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.tabletopFoldLayoutOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState

private val previewMembersTab: LazyListScope.() -> Unit = {}

/*
 * Why: the group's screen: its header, drawn once, over the tab picked, Despesas (the bills and payments by day),
 * Saldos (who owes whom) or Membros, which another module draws. Picking a tab only swaps what is under the tabs.
 */
@Composable
fun GroupDetailContent(
    state: GroupDetailUiState,
    snackbarHostState: SnackbarHostState,
    membersTab: LazyListScope.() -> Unit,
    onEvent: (GroupDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    // Why: the group whose row its avatar and name fly from into the bar; null where nothing flies.
    sharedGroupId: String? = null,
) {
    val money = rememberMoneyFormatter(state.header.currency)
    val onAction: (GroupAction) -> Unit = { action -> onEvent(eventOf(action)) }
    // Why: a tabletop draws Saldos its own way when the group is alone (6.2); beside the list it stays in the pane.
    val isTabletop = rememberFoldLayout().tabletopSplit != null
    if (isTabletop && state.tab == GroupTab.BALANCES && !LocalInDetailPane.current) {
        BalancesTabletopContent(
            state = state,
            snackbarHostState = snackbarHostState,
            money = money,
            onAction = onAction,
            onEvent = onEvent,
            modifier = modifier,
        )
        return
    }
    GroupTabScaffold(
        frame = GroupTabFrame(
            header = state.header,
            sharedGroupId = sharedGroupId,
            selectedTab = state.tab,
            hasBills = state.hasBills,
            isLoaded = state.isLoaded,
            isMyBalanceLoading = state.isMyBalanceLoading,
            isOnline = state.exit.isOnline,
            money = money,
            onCurrencySelected = { onEvent(GroupDetailUiEvent.OnCurrencySelected(it)) },
        ),
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        homeTab = state.homeTab,
        onHomeTabClick = { tab -> onEvent(GroupDetailUiEvent.OnRailTabClicked(tab)) },
        onSearchClick = { onEvent(GroupDetailUiEvent.OnRailSearchClicked) },
        body = when (state.tab) {
            GroupTab.EXPENSES -> expensesTabOf(state, onAction, onEvent)

            GroupTab.BALANCES -> balancesTabOf(state, money, onAction, onEvent)

            // Why: Membros draws only its rows; "Rachar conta" floats over it as over the other tabs.
            GroupTab.MEMBERS -> GroupTabBody(
                listContent = membersTab,
                floatingButton = splitBillButtonOf(state, onAction),
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun expensesTabOf(
    state: GroupDetailUiState,
    onAction: (GroupAction) -> Unit,
    onEvent: (GroupDetailUiEvent) -> Unit,
): GroupTabBody {
    val dates = rememberDateTimeTextFormatter()
    return GroupTabBody(
        listContent = {
            state.joinedAs?.let { name ->
                item(
                    key = "joined",
                    contentType = "joined",
                ) {
                    JoinedBanner(
                        name = name,
                        showsProtectAccess = state.showsProtectAccess,
                        onProtectAccessClick = { onEvent(GroupDetailUiEvent.OnProtectAccessClicked) },
                        onDismissClick = { onEvent(GroupDetailUiEvent.OnJoinedNoticeDismissed) },
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                        ),
                    )
                }
            }
            if (!state.isLoaded || state.isLoadingBills) {
                groupRowsSkeletonItem(
                    key = "bills-skeleton",
                    isWaitingForInternet = state.isWaitingForInternet,
                    isBadgeSquare = true,
                )
            } else if (state.isEmpty) {
                item(
                    key = "empty",
                    contentType = "empty",
                ) {
                    EmptyExpensesCard(onAction = onAction)
                }
            } else {
                expenseDayItems(
                    days = state.days,
                    dates = dates,
                    onEvent = onEvent,
                )
            }
        },
        floatingButton = splitBillButtonOf(state, onAction),
    )
}

@Composable
private fun balancesTabOf(
    state: GroupDetailUiState,
    money: MoneyFormatter,
    onAction: (GroupAction) -> Unit,
    onEvent: (GroupDetailUiEvent) -> Unit,
): GroupTabBody = GroupTabBody(
    listContent = {
        balanceItems(
            state = state,
            money = money,
            onAction = onAction,
            onEvent = onEvent,
        )
    },
    floatingButton = splitBillButtonOf(state, onAction),
)

// Why: beside the home's list, the rail's "Rachar conta" already marks this group.
@Composable
private fun splitBillButtonOf(
    state: GroupDetailUiState,
    onAction: (GroupAction) -> Unit,
): GroupTabFloatingButton? {
    val label = stringResource(R.string.group_detail_split_bill)
    if (!state.showsSplitBillButton || LocalInDetailPane.current) return null
    return GroupTabFloatingButton(
        icon = splitBillIcon(),
        label = label,
        onClick = { onAction(GroupAction.SPLIT_BILL) },
    )
}

private fun eventOf(action: GroupAction): GroupDetailUiEvent = when (action) {
    GroupAction.BACK -> GroupDetailUiEvent.OnBackClicked
    GroupAction.INVITE -> GroupDetailUiEvent.OnInviteClicked
    GroupAction.PHOTO -> GroupDetailUiEvent.OnPhotoClicked
    GroupAction.STATS -> GroupDetailUiEvent.OnStatsClicked
    GroupAction.PIN -> GroupDetailUiEvent.OnPinToggled
    GroupAction.EDIT_GROUP -> GroupDetailUiEvent.OnEditGroupClicked
    GroupAction.LEAVE_GROUP -> GroupDetailUiEvent.OnLeaveGroupClicked
    GroupAction.DELETE_GROUP -> GroupDetailUiEvent.OnDeleteGroupClicked
    GroupAction.MEMBERS -> GroupDetailUiEvent.OnTabSelected(GroupTab.MEMBERS)
    GroupAction.EXPENSES_TAB -> GroupDetailUiEvent.OnTabSelected(GroupTab.EXPENSES)
    GroupAction.BALANCES_TAB -> GroupDetailUiEvent.OnTabSelected(GroupTab.BALANCES)
    GroupAction.SPLIT_BILL -> GroupDetailUiEvent.OnSplitBillClicked
    GroupAction.SETTLE -> GroupDetailUiEvent.OnSettleClicked
}

@DevicePreviews
@Composable
private fun GroupDetailContentPreview() {
    BlitzSplitTheme {
        GroupDetailContent(
            state = createPreviewGroupDetailState(days = createPreviewDays()),
            snackbarHostState = SnackbarHostState(),
            membersTab = previewMembersTab,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupDetailContentEmptyPreview() {
    BlitzSplitTheme {
        GroupDetailContent(
            state = createPreviewGroupDetailState(days = emptyList()),
            snackbarHostState = SnackbarHostState(),
            membersTab = previewMembersTab,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupDetailContentLoadingPreview() {
    BlitzSplitTheme {
        GroupDetailContent(
            state = createPreviewGroupDetailState(days = emptyList()).copy(isLoaded = false),
            snackbarHostState = SnackbarHostState(),
            membersTab = previewMembersTab,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupDetailContentFirstDownloadPreview() {
    BlitzSplitTheme {
        GroupDetailContent(
            state = createPreviewGroupDetailState(days = emptyList()).copy(
                awaitsFullSync = true,
                isWaitingForInternet = true,
            ),
            snackbarHostState = SnackbarHostState(),
            membersTab = previewMembersTab,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupDetailContentJoinedPreview() {
    BlitzSplitTheme {
        GroupDetailContent(
            state = createPreviewGroupDetailState(days = createPreviewDays()).copy(
                joinedAs = "Bruno",
                isAccountUnprotected = true,
            ),
            snackbarHostState = SnackbarHostState(),
            membersTab = previewMembersTab,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupDetailContentBalancesPreview() {
    BlitzSplitTheme {
        GroupDetailContent(
            state = createPreviewBalancesState(),
            snackbarHostState = SnackbarHostState(),
            membersTab = previewMembersTab,
            onEvent = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupDetailContentSimplifiedPreview() {
    BlitzSplitTheme {
        GroupDetailContent(
            state = createPreviewSimplifiedBalancesState(),
            snackbarHostState = SnackbarHostState(),
            membersTab = previewMembersTab,
            onEvent = {},
        )
    }
}

@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun GroupDetailContentBalancesTabletopPreview() {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme {
            GroupDetailContent(
                state = createPreviewSimplifiedBalancesState(),
                snackbarHostState = SnackbarHostState(),
                membersTab = previewMembersTab,
                onEvent = {},
            )
        }
    }
}
