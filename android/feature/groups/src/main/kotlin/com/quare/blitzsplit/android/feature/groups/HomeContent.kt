package com.quare.blitzsplit.android.feature.groups

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.HomeNavigationBar
import com.quare.blitzsplit.android.designsystem.component.HomeTab
import com.quare.blitzsplit.android.designsystem.layout.AdaptiveScreen
import com.quare.blitzsplit.android.designsystem.layout.DeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.TopBottomHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiEvent
import com.quare.blitzsplit.feature.groups.presentation.model.HomeUiState

private val singleColumnMaxWidth = 624.dp
private val phoneFirstColumnWidth = 340.dp
private val tabletFirstColumnWidth = 520.dp
private val sectionSpacing = 12.dp
private val fabClearance = 96.dp
private val paneBottomMargin = 24.dp

private fun eventOf(tab: HomeTab): HomeUiEvent? = when (tab) {
    HomeTab.HOME -> null
    HomeTab.GROUPS -> HomeUiEvent.OnGroupsTabClicked
    HomeTab.BALANCES -> HomeUiEvent.OnBalancesTabClicked
    HomeTab.CONTACTS -> HomeUiEvent.OnContactsTabClicked
    HomeTab.ACTIVITY -> HomeUiEvent.OnActivityTabClicked
}

/*
 * Why: "Início" (flow 10), the home's first tab. No visible title, since the selected tab already names it. A phone
 * scrolls one column under the top bar with the profile, "Rachar conta" floating; anything wider has the rail with
 * "Rachar conta" on top and lays the sections out as the canvas draws its size (HomeLayout). The first use only shows
 * the card of 1.3; a Flip's cover screen only the balance and the latest bill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = stringResource(R.string.groups_home_title)
    val homeLayout = rememberHomeLayout()
    val metrics = rememberHomeMetrics(homeLayout)
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    AdaptiveScreen(modifier = modifier.fillMaxSize().semantics { paneTitle = title }) { layout ->
        if (homeLayout == HomeLayout.COVER) {
            HomeCoverPane(state = state)
            return@AdaptiveScreen
        }
        // Why: the rail runs edge to edge beside the scaffold, which then leaves out the start inset it takes.
        Row(modifier = Modifier.fillMaxSize()) {
            if (homeLayout.usesRail) {
                SplitBillNavigationRail(
                    selected = HomeTab.HOME,
                    onTabClick = { tab -> eventOf(tab)?.let(onEvent) },
                    onSplitBillClick = {
                        onEvent(
                            HomeUiEvent.OnSplitBillClicked,
                        )
                    }.takeIf { state.showsSplitBillButton },
                )
            }
            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    if (homeLayout.usesTopBar || state.showsFirstUse) {
                        HomeTopBar(
                            state = state,
                            onEvent = onEvent,
                            scrollBehavior = scrollBehavior,
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    if (!homeLayout.usesRail && state.showsSplitBillButton) {
                        ExtendedFloatingActionButton(
                            onClick = { onEvent(HomeUiEvent.OnSplitBillClicked) },
                            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                            text = { Text(stringResource(R.string.groups_split_bill)) },
                            expanded = !scrollState.canScrollBackward,
                        )
                    }
                },
                bottomBar = {
                    if (!homeLayout.usesRail) {
                        HomeNavigationBar(
                            selected = HomeTab.HOME,
                            onTabClick = { tab -> eventOf(tab)?.let(onEvent) },
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentWindowInsets = if (homeLayout.usesRail) {
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                } else {
                    WindowInsets.safeDrawing
                },
            ) { padding ->
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(HomeUiEvent.OnRefresh) },
                    modifier = Modifier
                        .padding(padding)
                        .consumeWindowInsets(padding),
                ) {
                    HomePane(
                        state = state,
                        onEvent = onEvent,
                        homeLayout = homeLayout,
                        layout = layout,
                        metrics = metrics,
                        scrollState = scrollState,
                    )
                }
            }
        }
    }
}

// Why: nothing until Room is read, so the first use's card never flashes before the groups.
@Composable
private fun HomePane(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    homeLayout: HomeLayout,
    layout: DeviceLayout,
    metrics: HomeMetrics,
    scrollState: ScrollState,
) {
    when {
        state.showsFirstUse -> EmptyGroupsPane(
            layout = layout,
            showsCard = true,
            onCreateGroup = { onEvent(HomeUiEvent.OnCreateGroupClicked) },
            onJoinWithLink = { onEvent(HomeUiEvent.OnJoinWithLinkClicked) },
            body = R.string.groups_home_first_use_body,
        ) {
            HomeSyncStatusIndicator(
                state = state,
                onEvent = onEvent,
            )
        }

        !state.showsHome -> Unit

        homeLayout == HomeLayout.TWO_COLUMNS -> Row(modifier = Modifier.fillMaxSize()) {
            val firstWidth = if (metrics.isCompact) phoneFirstColumnWidth else tabletFirstColumnWidth
            ScrollingColumn(modifier = Modifier.width(firstWidth + metrics.gutter * 2)) {
                HomeFirstColumn(state, onEvent, metrics)
            }
            ScrollingColumn(modifier = Modifier.weight(1f)) {
                HomeSecondColumn(state, onEvent, metrics)
            }
        }

        // Why: the book's halves meet on the hinge, which neither column crosses.
        homeLayout == HomeLayout.BOOK -> TwoHalvesLayout(
            start = { ScrollingColumn { HomeFirstColumn(state, onEvent, metrics) } },
            end = { ScrollingColumn { HomeSecondColumn(state, onEvent, metrics) } },
            modifier = Modifier.fillMaxSize(),
        )

        // Why: what is read above the hinge (the people, the balance, the groups), what is tapped below it.
        homeLayout == HomeLayout.TABLETOP -> TopBottomHalvesLayout(
            top = {
                Row(modifier = Modifier.fillMaxSize()) {
                    ScrollingColumn(modifier = Modifier.weight(1f)) {
                        HomeColumnHeader(state, onEvent, metrics)
                        HomePeopleSection(state, onEvent, metrics)
                        HomeSummaryCard(
                            state = state,
                            onEvent = onEvent,
                            metrics = metrics,
                            modifier = Modifier.padding(horizontal = metrics.gutter),
                        )
                    }
                    ScrollingColumn(modifier = Modifier.weight(1f)) {
                        HomeGroupsSection(state, onEvent, metrics)
                    }
                }
            },
            bottom = {
                Row(modifier = Modifier.fillMaxSize()) {
                    ScrollingColumn(modifier = Modifier.weight(1f)) {
                        HomeShortcutsSection(state, onEvent, metrics)
                    }
                    ScrollingColumn(modifier = Modifier.weight(1f)) {
                        HomeBillsSection(state, onEvent, metrics)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        else -> HomeSingleColumn(
            state = state,
            onEvent = onEvent,
            metrics = metrics,
            scrollState = scrollState,
        )
    }
}

// Why: a tablet or a foldable upright centers the phone's column, 624dp at most.
@Composable
private fun HomeSingleColumn(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
    scrollState: ScrollState,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = singleColumnMaxWidth)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing),
        ) {
            HomeSyncStatusIndicator(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.padding(horizontal = metrics.gutter),
            )
            HomePeopleSection(state, onEvent, metrics)
            HomeSummaryCard(
                state = state,
                onEvent = onEvent,
                metrics = metrics,
                modifier = Modifier.padding(horizontal = metrics.gutter),
            )
            HomeShortcutsSection(state, onEvent, metrics)
            HomeGroupsSection(state, onEvent, metrics)
            HomeBillsSection(state, onEvent, metrics)
            BottomClearanceSpacer(fabClearance)
        }
    }
}

// Why: beside the rail, the status, the people, the balance and the shortcuts.
@Composable
private fun ColumnScope.HomeFirstColumn(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
) {
    HomeColumnHeader(state, onEvent, metrics)
    HomePeopleSection(state, onEvent, metrics)
    HomeSummaryCard(
        state = state,
        onEvent = onEvent,
        metrics = metrics,
        modifier = Modifier.padding(horizontal = metrics.gutter),
    )
    HomeShortcutsSection(state, onEvent, metrics)
}

// Why: the groups and the latest bills, a little lower, level with the first column's sections.
@Composable
private fun ColumnScope.HomeSecondColumn(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
) {
    HomeGroupsSection(state, onEvent, metrics)
    HomeBillsSection(state, onEvent, metrics)
}

// Why: the shortcuts, and the recovery code's card under them while it shows.
@Composable
private fun HomeShortcutsSection(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
) {
    HomeShortcutsRow(
        state = state,
        onEvent = onEvent,
        metrics = metrics,
        modifier = Modifier.padding(horizontal = metrics.gutter),
    )
    if (state.showsRecoveryCodeCard) {
        HomeRecoveryCodeCard(
            onEvent = onEvent,
            modifier = Modifier.padding(horizontal = metrics.gutter),
        )
    }
}

// Why: with no top bar beside the rail, the first column starts with the sync chip and the profile.
@Composable
private fun HomeColumnHeader(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    metrics: HomeMetrics,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(
                start = metrics.gutter,
                end = metrics.gutter - 4.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            HomeSyncStatusIndicator(
                state = state,
                onEvent = onEvent,
            )
        }
        ProfileAvatarButton(
            initial = state.avatarInitial,
            color = state.avatarColor,
            photoUrl = state.photoUrl,
            onClick = { onEvent(HomeUiEvent.OnProfileClicked) },
        )
    }
}

// Why: each column scrolls on its own; a pull on either feeds the pull to refresh around them.
@Composable
private fun ScrollingColumn(
    modifier: Modifier = Modifier,
    bottomMargin: Dp = paneBottomMargin,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(
                top = 8.dp,
                bottom = bottomMargin,
            ),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = {},
        actions = {
            ProfileAvatarButton(
                initial = state.avatarInitial,
                color = state.avatarColor,
                photoUrl = state.photoUrl,
                onClick = { onEvent(HomeUiEvent.OnProfileClicked) },
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        scrollBehavior = scrollBehavior,
    )
}
