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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.MyBalanceCard
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.feature.groupdetail.presentation.model.GroupHeader

private val singleColumnMaxWidth = 624.dp
private val twoColumnsMaxWidth = 1100.dp
private val phoneSideColumnWidth = 320.dp
private val tabletSideColumnWidth = 380.dp
private val fabClearance = 96.dp

// Why: what Despesas and Saldos draw alike around their own list.
internal class GroupTabFrame(
    val header: GroupHeader,
    val selectedTab: GroupTab,
    // Why: "nenhuma despesa ainda" in the bar before the first bill.
    val hasBills: Boolean,
    val isLoaded: Boolean,
    val showsSplitBillButton: Boolean,
    // Why: "Sair do grupo" and "Apagar grupo" need the internet, which the ⋮ menu says.
    val isOnline: Boolean,
    val money: MoneyFormatter,
)

/*
 * Why: a phone upright stacks the faces, the balance card, the tabs and the list under the bar, with "Rachar conta"
 * floating; lying down, and on a tablet lying down, the faces, the card and the tabs take a column next to the list.
 * A tablet upright centers one column.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupTabScaffold(
    frame: GroupTabFrame,
    snackbarHostState: SnackbarHostState,
    onAction: (GroupAction) -> Unit,
    modifier: Modifier = Modifier,
    listContent: LazyListScope.() -> Unit,
) {
    val layout = rememberDeviceLayout()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
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
            if (frame.showsSplitBillButton) {
                ExtendedFloatingActionButton(
                    onClick = { onAction(GroupAction.SPLIT_BILL) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.group_detail_split_bill)) },
                    expanded = !listState.canScrollBackward,
                )
            }
        },
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
                        content = listContent,
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
                    listContent()
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
