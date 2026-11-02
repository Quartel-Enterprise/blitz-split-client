package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupTabBody
import com.quare.blitzsplit.android.designsystem.component.GroupTabFloatingButton
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.LocalInDetailPane
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState

private val previewMembersTab = GroupTabBody(
    listContent = {},
    floatingButton = null,
)

/*
 * Why: the group's screen: its header, drawn once, over the tab picked, Despesas (the bills and payments by day),
 * Saldos (who owes whom) or Membros, which another module draws. Picking a tab only swaps what is under the tabs.
 */
@Composable
fun GroupDetailContent(
    state: GroupDetailUiState,
    snackbarHostState: SnackbarHostState,
    membersTab: GroupTabBody,
    onEvent: (GroupDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(state.header.currency)
    val onAction: (GroupAction) -> Unit = { action -> onEvent(eventOf(action)) }
    GroupTabScaffold(
        frame = GroupTabFrame(
            header = state.header,
            selectedTab = state.tab,
            hasBills = state.hasBills,
            isLoaded = state.isLoaded,
            isOnline = state.exit.isOnline,
            money = money,
        ),
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        homeTab = state.homeTab,
        activityBadgeCount = state.activityUnreadCount,
        onHomeTabClick = { tab -> onEvent(GroupDetailUiEvent.OnRailTabClicked(tab)) },
        body = when (state.tab) {
            GroupTab.EXPENSES -> expensesTabOf(state, money, onAction, onEvent)
            GroupTab.BALANCES -> balancesTabOf(state, money, onAction, onEvent)
            GroupTab.MEMBERS -> membersTab
        },
        modifier = modifier,
    )
}

@Composable
private fun expensesTabOf(
    state: GroupDetailUiState,
    money: MoneyFormatter,
    onAction: (GroupAction) -> Unit,
    onEvent: (GroupDetailUiEvent) -> Unit,
): GroupTabBody {
    val dates = rememberDateTimeTextFormatter()
    return GroupTabBody(
        listContent = {
            state.joinedAs?.let { name ->
                item(key = "joined") {
                    JoinedBanner(
                        name = name,
                        showsProtectAccess = state.showsProtectAccess,
                        onProtectAccessClick = { onEvent(GroupDetailUiEvent.OnProtectAccessClicked) },
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                        ),
                    )
                }
            }
            if (state.isEmpty) {
                item(key = "empty") { EmptyExpensesCard(onAction = onAction) }
            } else {
                expenseDayItems(
                    days = state.days,
                    money = money,
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
        icon = Icons.Outlined.Add,
        label = label,
        position = FabPosition.End,
        onClick = { onAction(GroupAction.SPLIT_BILL) },
    )
}

private fun eventOf(action: GroupAction): GroupDetailUiEvent = when (action) {
    GroupAction.BACK -> GroupDetailUiEvent.OnBackClicked
    GroupAction.INVITE -> GroupDetailUiEvent.OnInviteClicked
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
