package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiEvent
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupDetailUiState

// Why: "Despesas do grupo", the bills and payments by day under the group's header and tabs.
@Composable
fun GroupDetailContent(
    state: GroupDetailUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (GroupDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = rememberMoneyFormatter(state.header.currency)
    val dates = rememberDateTimeTextFormatter()
    val onAction: (GroupAction) -> Unit = { action -> eventOf(action)?.let(onEvent) }
    GroupTabScaffold(
        frame = GroupTabFrame(
            header = state.header,
            selectedTab = GroupTab.EXPENSES,
            hasBills = state.days.isNotEmpty(),
            isLoaded = state.isLoaded,
            showsSplitBillButton = state.showsSplitBillButton,
            isOnline = state.exit.isOnline,
            money = money,
        ),
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        homeTab = state.homeTab,
        onHomeTabClick = { tab -> onEvent(GroupDetailUiEvent.OnRailTabClicked(tab)) },
        modifier = modifier,
    ) {
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
    }
}

private fun eventOf(action: GroupAction): GroupDetailUiEvent? = when (action) {
    GroupAction.BACK -> GroupDetailUiEvent.OnBackClicked
    GroupAction.INVITE -> GroupDetailUiEvent.OnInviteClicked
    GroupAction.STATS -> GroupDetailUiEvent.OnStatsClicked
    GroupAction.PIN -> GroupDetailUiEvent.OnPinToggled
    GroupAction.EDIT_GROUP -> GroupDetailUiEvent.OnEditGroupClicked
    GroupAction.LEAVE_GROUP -> GroupDetailUiEvent.OnLeaveGroupClicked
    GroupAction.DELETE_GROUP -> GroupDetailUiEvent.OnDeleteGroupClicked
    GroupAction.MEMBERS -> GroupDetailUiEvent.OnMembersClicked
    GroupAction.EXPENSES_TAB -> null
    GroupAction.BALANCES_TAB -> GroupDetailUiEvent.OnBalancesTabClicked
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
            onEvent = {},
        )
    }
}
