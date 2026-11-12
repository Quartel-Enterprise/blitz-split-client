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
import com.quare.blitzsplit.android.designsystem.component.CurrencyChipRow
import com.quare.blitzsplit.android.designsystem.component.MyBalanceCard
import com.quare.blitzsplit.android.designsystem.component.WithGroupNavigationRail
import com.quare.blitzsplit.android.designsystem.component.rememberCollapsingTopBarScrollBehavior
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.core.model.GroupCurrency
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
    // Why: the group whose row its avatar and name fly from into the bar; null where nothing flies.
    val sharedGroupId: String?,
    val selectedTab: GroupTab,
    // Why: "nenhuma despesa ainda" in the bar before the first bill.
    val hasBills: Boolean,
    val isLoaded: Boolean,
    // Why: "Seu saldo neste grupo" shimmers while the account's copy isn't whole, rather than hiding as if settled.
    val isMyBalanceLoading: Boolean,
    // Why: "Sair do grupo" and "Apagar grupo" need the internet, which the ⋮ menu says.
    val isOnline: Boolean,
    val money: MoneyFormatter,
    // Why: a chip per currency the group has money in, drawn over the amounts while there are two or more.
    val onCurrencySelected: (GroupCurrency) -> Unit,
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
    onHomeTabClick: (HomeTab) -> Unit,
    onSearchClick: () -> Unit,
    // Why: the picked tab's rows and button; only these change when another tab is picked.
    body: GroupTabBody,
    modifier: Modifier = Modifier,
) {
    WithGroupNavigationRail(
        selected = homeTab,
        onTabClick = onHomeTabClick,
        onSearchClick = onSearchClick,
        modifier = modifier,
    ) {
        GroupTabScaffoldContent(
            frame = frame,
            snackbarHostState = snackbarHostState,
            onAction = onAction,
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
    body: GroupTabBody,
) {
    val layout = rememberDeviceLayout()
    val listState = rememberLazyListState()
    /*
     * Why: on a phone upright the bar folds to its one line on a scroll down and opens again on a scroll up, wherever
     * the list is; anywhere wider it opens only back at the top. Material's two-line bars never leave whole, which
     * keeps back and the group's actions in reach.
     */
    val scrollBehavior = rememberCollapsingTopBarScrollBehavior(
        collapses = !layout.usesNavigationRail && frame.isLoaded,
        canScroll = { listState.canScrollForward || listState.canScrollBackward },
        otherwise = { state -> TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state) },
    )
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (frame.isLoaded) {
                GroupTopBar(
                    header = frame.header,
                    hasBills = frame.hasBills,
                    isOnline = frame.isOnline,
                    money = frame.money,
                    scrollBehavior = scrollBehavior,
                    onAction = onAction,
                    sharedGroupId = frame.sharedGroupId,
                )
            } else {
                GroupLoadingTopBar(onAction)
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
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

// Why: the faces, "Seu saldo neste grupo" while there is a balance, and the tabs under them; shimmering until read.
@Composable
private fun GroupSideHeader(
    frame: GroupTabFrame,
    onAction: (GroupAction) -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    if (!frame.isLoaded) {
        GroupHeaderSkeleton(horizontalPadding = horizontalPadding)
        return
    }
    val balancePadding = Modifier.padding(
        start = horizontalPadding,
        end = horizontalPadding,
        top = 12.dp,
    )
    Column {
        GroupFacesRow(
            header = frame.header,
            onAction = onAction,
            modifier = modifier,
        )
        val balance = frame.header.myBalance
        if (frame.isMyBalanceLoading) {
            MyBalanceSkeleton(modifier = balancePadding)
        }
        // Why: Membros has its own chips over its balances, so the header's stay out of that tab.
        if (frame.selectedTab != GroupTab.MEMBERS) {
            CurrencyChipRow(
                currencies = frame.header.currencies,
                selected = frame.header.currency,
                onSelected = frame.onCurrencySelected,
                modifier = balancePadding,
            )
        }
        balance?.let {
            MyBalanceCard(
                balance = it,
                isEveryoneSettled = frame.header.isEveryoneSettled,
                money = frame.money,
                onSettleClick = { onAction(GroupAction.SETTLE) },
                modifier = balancePadding,
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
