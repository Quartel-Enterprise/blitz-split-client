package com.quare.blitzsplit.android.feature.groupdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.GroupTabBody
import com.quare.blitzsplit.android.designsystem.component.MyBalanceCard
import com.quare.blitzsplit.android.designsystem.component.WithGroupNavigationRail
import com.quare.blitzsplit.android.designsystem.component.toGroupRailInsets
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.core.model.GroupTab
import com.quare.blitzsplit.core.model.HomeTab
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader

private val singleColumnMaxWidth = 624.dp
private val twoColumnsMaxWidth = 1100.dp
private val phoneSideColumnWidth = 320.dp
private val tabletSideColumnWidth = 380.dp
private val fabClearance = 96.dp

// Why: the group's header, the same over every tab.
internal class GroupTabFrame(
    val header: GroupHeader,
    val selectedTab: GroupTab,
    // Why: "nenhuma despesa ainda" in the bar before the first bill.
    val hasBills: Boolean,
    val isLoaded: Boolean,
    // Why: "Sair do grupo" and "Apagar grupo" need the internet, which the ⋮ menu says.
    val isOnline: Boolean,
    val money: MoneyFormatter,
)

/*
 * Why: a phone upright stacks the faces, the balance card, the tabs and the tab's rows under the bar, with its button
 * floating; lying down, and on a tablet lying down, the faces, the card and the tabs take a column next to the list.
 * A tablet upright centers one column. Drawn alone anywhere wider than a phone upright, the home's rail stays beside
 * it, as the canvas draws it (6.1).
 */
@Composable
internal fun GroupTabScaffold(
    frame: GroupTabFrame,
    snackbarHostState: SnackbarHostState,
    onAction: (GroupAction) -> Unit,
    // Why: the home's rail beside the group drawn alone (WithGroupNavigationRail), on the tab it was opened from.
    homeTab: HomeTab,
    activityBadgeCount: Int,
    onHomeTabClick: (HomeTab) -> Unit,
    // Why: the picked tab's rows and button; only these change when another tab is picked.
    body: GroupTabBody,
    modifier: Modifier = Modifier,
) {
    WithGroupNavigationRail(
        selected = homeTab,
        onTabClick = onHomeTabClick,
        activityBadgeCount = activityBadgeCount,
        modifier = modifier,
    ) { showsRail ->
        GroupTabScaffoldContent(
            frame = frame,
            snackbarHostState = snackbarHostState,
            onAction = onAction,
            showsRail = showsRail,
            body = body,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupTabScaffoldContent(
    frame: GroupTabFrame,
    snackbarHostState: SnackbarHostState,
    onAction: (GroupAction) -> Unit,
    showsRail: Boolean,
    body: GroupTabBody,
) {
    val layout = rememberDeviceLayout()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.toGroupRailInsets(showsRail),
        topBar = {
            if (frame.isLoaded) {
                GroupTopBar(
                    header = frame.header,
                    hasBills = frame.hasBills,
                    isOnline = frame.isOnline,
                    money = frame.money,
                    scrollBehavior = scrollBehavior,
                    onAction = onAction,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            body.floatingButton?.let { button ->
                ExtendedFloatingActionButton(
                    onClick = button.onClick,
                    icon = { Icon(button.icon, contentDescription = null) },
                    text = { Text(button.label) },
                    expanded = !listState.canScrollBackward,
                )
            }
        },
        floatingActionButtonPosition = body.floatingButton?.position ?: FabPosition.End,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!frame.isLoaded) return@Box
            if (layout.usesTwoColumns) {
                Row(
                    modifier = Modifier
                        .widthIn(max = twoColumnsMaxWidth)
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .width(if (layout.isTablet) tabletSideColumnWidth else phoneSideColumnWidth)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 12.dp,
                                bottom = 24.dp,
                            ),
                    ) {
                        GroupSideHeader(
                            frame = frame,
                            onAction = onAction,
                            horizontalPadding = 4.dp,
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = listState,
                        contentPadding = PaddingValues(bottom = fabClearance),
                        content = body.listContent,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = singleColumnMaxWidth)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = fabClearance),
                ) {
                    item(key = "header") {
                        GroupSideHeader(
                            frame = frame,
                            onAction = onAction,
                            horizontalPadding = 16.dp,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                    body.listContent(this)
                }
            }
        }
    }
}

// Why: the faces, "Seu saldo neste grupo" while there is a balance, and the tabs under them.
@Composable
private fun GroupSideHeader(
    frame: GroupTabFrame,
    onAction: (GroupAction) -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Column {
        GroupFacesRow(
            header = frame.header,
            onAction = onAction,
            modifier = modifier,
        )
        frame.header.myBalance?.let { balance ->
            MyBalanceCard(
                balance = balance,
                isEveryoneSettled = frame.header.isEveryoneSettled,
                money = frame.money,
                onSettleClick = { onAction(GroupAction.SETTLE) },
                modifier = Modifier.padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 12.dp,
                ),
            )
        }
        GroupTabsRow(
            selectedTab = frame.selectedTab,
            onAction = onAction,
            modifier = Modifier.padding(
                start = horizontalPadding,
                end = horizontalPadding,
                top = 12.dp,
            ),
        )
    }
}
