package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.component.WithGroupNavigationRail
import com.quare.blitzsplit.android.designsystem.component.toGroupRailInsets
import com.quare.blitzsplit.android.designsystem.format.DateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.MoneyFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.LocalFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.LocalInDetailPane
import com.quare.blitzsplit.android.designsystem.layout.TopBottomHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.TwoHalvesLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.layout.rememberFoldLayout
import com.quare.blitzsplit.android.designsystem.layout.tabletopFoldLayoutOf
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState

private val singleColumnMaxWidth = 624.dp
private val twoColumnsMaxWidth = 1100.dp
private val twoColumnsPieSize = 112.dp
private val tabletopChartHeight = 72.dp
private val tabletopSideMargin = 48.dp

// Why: how the canvas lays Estatísticas out at each size (6.6).
private enum class StatsLayout {
    // Why: a phone upright, a tablet and a foldable upright: one scrolling column.
    COLUMN,

    /*
     * Why: a phone and a tablet lying down, in the group's pane beside the groups too: the total and Por categoria in
     * the first column, Ao longo do tempo on top of the second, so it shows without scrolling, then Por pessoa and
     * Maior despesa.
     */
    COLUMNS,

    // Why: a book: the same two columns, one per half of the hinge.
    BOOK,

    /*
     * Why: a tabletop: the total and Ao longo do tempo in the top half, Por categoria and Por pessoa below, scrolling;
     * the grains' note and Maior despesa go, so the top half fits.
     */
    TABLETOP,
}

@Composable
private fun rememberStatsLayout(): StatsLayout {
    val fold = rememberFoldLayout()
    val usesTwoColumns = rememberDeviceLayout().usesTwoColumns
    return when {
        fold.tabletopSplit != null -> StatsLayout.TABLETOP

        fold.bookSplit != null -> StatsLayout.BOOK

        // Why: the pane lays a group's screen out as a tablet upright, but the canvas draws Estatísticas there in two.
        usesTwoColumns || LocalInDetailPane.current -> StatsLayout.COLUMNS

        else -> StatsLayout.COLUMN
    }
}

/*
 * Why: "Estatísticas" (6.6) under the medium flexible bar, with Back and no "Rachar conta", laid out per size
 * (StatsLayout). Anywhere wider than a phone upright the home's rail stays beside it, as the canvas draws it, but on a
 * tabletop and in the group's pane beside the groups (a tablet lying down, decision 2).
 */
@Composable
fun GroupStatsContent(
    state: GroupStatsUiState,
    onEvent: (GroupStatsUiEvent) -> Unit,
    onPickDates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WithGroupNavigationRail(
        selected = state.homeTab,
        onTabClick = { tab -> onEvent(GroupStatsUiEvent.OnRailTabClicked(tab)) },
        activityBadgeCount = state.activityUnreadCount,
        modifier = modifier,
    ) { showsRail ->
        GroupStatsScaffold(
            state = state,
            onEvent = onEvent,
            onPickDates = onPickDates,
            showsRail = showsRail,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupStatsScaffold(
    state: GroupStatsUiState,
    onEvent: (GroupStatsUiEvent) -> Unit,
    onPickDates: () -> Unit,
    showsRail: Boolean,
) {
    val layout = rememberStatsLayout()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val money = rememberMoneyFormatter(state.currency)
    val dates = rememberStatsTextFormatter()
    val times = rememberDateTimeTextFormatter()
    val isTabletop = layout == StatsLayout.TABLETOP
    val periodChip = @Composable { chipModifier: Modifier ->
        if (state.isLoaded && state.showsPeriodPicker) {
            StatsPeriodChip(
                state = state,
                dates = dates,
                onEvent = onEvent,
                onPickDates = onPickDates,
                modifier = chipModifier,
                showsRange = !isTabletop,
            )
        }
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(if (isTabletop) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.toGroupRailInsets(showsRail),
        topBar = {
            val title = @Composable { Text(stringResource(R.string.group_stats_title)) }
            val subtitle = @Composable {
                Text(
                    text = state.groupName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val back = @Composable {
                IconButton(onClick = { onEvent(GroupStatsUiEvent.OnBackClicked) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.group_stats_back, state.groupName),
                    )
                }
            }
            // Why: on a tabletop the bar stays small, the period in it, so the top half keeps room for the chart.
            if (isTabletop) {
                TopAppBar(
                    title = title,
                    subtitle = subtitle,
                    navigationIcon = back,
                    actions = { periodChip(Modifier.padding(end = 12.dp)) },
                )
            } else {
                MediumFlexibleTopAppBar(
                    title = title,
                    subtitle = subtitle,
                    navigationIcon = back,
                    scrollBehavior = scrollBehavior,
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
            val chip = @Composable {
                if (!isTabletop) {
                    periodChip(
                        Modifier.padding(
                            top = 12.dp,
                            bottom = 12.dp,
                        ),
                    )
                }
            }
            val empty = @Composable {
                Column(
                    modifier = Modifier
                        .widthIn(max = singleColumnMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    chip()
                    if (!state.isLoaded || state.isLoadingBills) {
                        StatsSkeleton(
                            isWaitingForInternet = state.isWaitingForInternet,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    } else {
                        StatsEmptyCard(
                            state = state,
                            dates = dates,
                            onEvent = onEvent,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            when {
                // Why: a book keeps the empty card off the hinge, in the half before it.
                !state.showsStats && layout == StatsLayout.BOOK -> TwoHalvesLayout(
                    start = { empty() },
                    end = {},
                    modifier = Modifier.fillMaxSize(),
                )

                // Why: a tabletop keeps it above the hinge, in the top half.
                !state.showsStats && layout == StatsLayout.TABLETOP -> TopBottomHalvesLayout(
                    top = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) { empty() }
                    },
                    bottom = {},
                    modifier = Modifier.fillMaxSize(),
                )

                !state.showsStats -> empty()

                layout == StatsLayout.COLUMNS -> Row(
                    modifier = Modifier
                        .widthIn(max = twoColumnsMaxWidth)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    StatsSummaryColumn(
                        state = state,
                        money = money,
                        chip = chip,
                        modifier = Modifier.weight(1f),
                    )
                    StatsOverTimeColumn(
                        state = state,
                        money = money,
                        dates = dates,
                        times = times,
                        onEvent = onEvent,
                        modifier = Modifier.weight(1f),
                    )
                }

                layout == StatsLayout.BOOK -> TwoHalvesLayout(
                    start = {
                        StatsSummaryColumn(
                            state = state,
                            money = money,
                            chip = chip,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = 16.dp,
                                    end = 12.dp,
                                ),
                        )
                    },
                    end = {
                        StatsOverTimeColumn(
                            state = state,
                            money = money,
                            dates = dates,
                            times = times,
                            onEvent = onEvent,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = 16.dp,
                                    end = 24.dp,
                                ),
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                layout == StatsLayout.TABLETOP -> StatsTabletopLayout(
                    state = state,
                    money = money,
                    dates = dates,
                    onEvent = onEvent,
                )

                else -> StatsColumn(
                    state = state,
                    money = money,
                    dates = dates,
                    times = times,
                    onEvent = onEvent,
                    chip = chip,
                )
            }
        }
    }
}

// Why: a phone upright, a tablet and a foldable upright scroll everything in one column.
@Composable
private fun StatsColumn(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    dates: StatsTextFormatter,
    times: DateTimeTextFormatter,
    onEvent: (GroupStatsUiEvent) -> Unit,
    chip: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = singleColumnMaxWidth)
            .fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 24.dp,
        ),
    ) {
        item(key = "period") { chip() }
        item(key = "summary") {
            StatsSummaryHero(
                state = state,
                money = money,
            )
        }
        item(key = "categories") {
            StatsCategoriesSection(
                state = state,
                money = money,
            )
        }
        item(key = "people") {
            StatsPeopleSection(
                state = state,
                money = money,
                onEvent = onEvent,
            )
        }
        item(key = "time") {
            StatsOverTimeSection(
                state = state,
                money = money,
                dates = dates,
                onEvent = onEvent,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item(key = "biggest") {
            StatsBiggestSection(
                state = state,
                money = money,
                times = times,
                onEvent = onEvent,
            )
        }
    }
}

// Why: the first of two columns, or a book's half before the hinge: the period, the total and Por categoria.
@Composable
private fun StatsSummaryColumn(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    chip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        chip()
        StatsSummaryHero(
            state = state,
            money = money,
        )
        StatsCategoriesSection(
            state = state,
            money = money,
            pieSize = twoColumnsPieSize,
        )
    }
}

// Why: the second of two columns, or a book's half after the hinge: Ao longo do tempo, Por pessoa and Maior despesa.
@Composable
private fun StatsOverTimeColumn(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    dates: StatsTextFormatter,
    times: DateTimeTextFormatter,
    onEvent: (GroupStatsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        StatsOverTimeSection(
            state = state,
            money = money,
            dates = dates,
            onEvent = onEvent,
        )
        StatsPeopleSection(
            state = state,
            money = money,
            onEvent = onEvent,
        )
        StatsBiggestSection(
            state = state,
            money = money,
            times = times,
            onEvent = onEvent,
        )
    }
}

/*
 * Why: a tabletop: the total and Ao longo do tempo side by side, down against the hinge in the top half; Por categoria
 * and Por pessoa side by side in the bottom half, scrolling under the thumb.
 */
@Composable
private fun StatsTabletopLayout(
    state: GroupStatsUiState,
    money: MoneyFormatter,
    dates: StatsTextFormatter,
    onEvent: (GroupStatsUiEvent) -> Unit,
) {
    TopBottomHalvesLayout(
        top = {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val halfHeight = maxHeight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = halfHeight)
                        .padding(horizontal = tabletopSideMargin),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        StatsSummaryHero(
                            state = state,
                            money = money,
                            modifier = Modifier.weight(1f),
                        )
                        StatsOverTimeSection(
                            state = state,
                            money = money,
                            dates = dates,
                            onEvent = onEvent,
                            modifier = Modifier.weight(1f),
                            chartHeight = tabletopChartHeight,
                            showsGrainNote = false,
                        )
                    }
                }
            }
        },
        bottom = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = tabletopSideMargin,
                        end = tabletopSideMargin,
                        bottom = 24.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                StatsCategoriesSection(
                    state = state,
                    money = money,
                    pieSize = twoColumnsPieSize,
                    modifier = Modifier.weight(1f),
                )
                StatsPeopleSection(
                    state = state,
                    money = money,
                    onEvent = onEvent,
                    modifier = Modifier.weight(1f),
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@DevicePreviews
@Composable
private fun GroupStatsContentPreview() {
    BlitzSplitTheme {
        GroupStatsContent(
            state = createPreviewGroupStatsState(),
            onEvent = {},
            onPickDates = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupStatsContentEmptyPreview() {
    BlitzSplitTheme {
        GroupStatsContent(
            state = createPreviewGroupStatsState(hasBills = false),
            onEvent = {},
            onPickDates = {},
        )
    }
}

@DevicePreviews
@Composable
private fun GroupStatsContentFirstDownloadPreview() {
    BlitzSplitTheme {
        GroupStatsContent(
            state = createPreviewGroupStatsState(hasBills = false).copy(
                isFirstDownload = true,
                isWaitingForInternet = true,
            ),
            onEvent = {},
            onPickDates = {},
        )
    }
}

// Why: a preview's window reports no fold, so the tabletop's hinge is handed in.
@Preview(
    name = "Tabletop",
    device = "spec:width=841dp,height=673dp,orientation=landscape",
)
@Composable
private fun GroupStatsContentTabletopPreview() {
    CompositionLocalProvider(LocalFoldLayout provides tabletopFoldLayoutOf(841.dp, 673.dp)) {
        BlitzSplitTheme {
            GroupStatsContent(
                state = createPreviewGroupStatsState(),
                onEvent = {},
                onPickDates = {},
            )
        }
    }
}
