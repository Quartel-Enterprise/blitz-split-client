package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.android.designsystem.format.rememberDateTimeTextFormatter
import com.quare.blitzsplit.android.designsystem.format.rememberMoneyFormatter
import com.quare.blitzsplit.android.designsystem.layout.DevicePreviews
import com.quare.blitzsplit.android.designsystem.layout.rememberDeviceLayout
import com.quare.blitzsplit.android.designsystem.theme.BlitzSplitTheme
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState

private val singleColumnMaxWidth = 624.dp
private val twoColumnsMaxWidth = 1100.dp

/*
 * Why: "Estatísticas" (6.6) under the medium flexible bar, with Back and no "Rachar conta". A phone upright, and a
 * tablet upright, scroll one column: the period, the total, Por pessoa, Ao longo do tempo and Maior despesa. Lying
 * down, the total and Por pessoa take the first column, and Ao longo do tempo and Maior despesa the second.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupStatsContent(
    state: GroupStatsUiState,
    onEvent: (GroupStatsUiEvent) -> Unit,
    onPickDates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = rememberDeviceLayout()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val money = rememberMoneyFormatter(state.currency)
    val dates = rememberStatsTextFormatter()
    val times = rememberDateTimeTextFormatter()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(R.string.group_stats_title)) },
                subtitle = {
                    Text(
                        text = state.groupName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(GroupStatsUiEvent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.group_stats_back, state.groupName),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!state.isLoaded) return@Box
            val chip = @Composable {
                if (state.showsPeriodPicker) {
                    StatsPeriodChip(
                        state = state,
                        dates = dates,
                        onEvent = onEvent,
                        onPickDates = onPickDates,
                        modifier = Modifier.padding(
                            top = 12.dp,
                            bottom = 12.dp,
                        ),
                    )
                }
            }
            when {
                !state.showsStats -> Column(
                    modifier = Modifier
                        .widthIn(max = singleColumnMaxWidth)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    chip()
                    StatsEmptyCard(
                        state = state,
                        dates = dates,
                        onEvent = onEvent,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                layout.usesTwoColumns -> Row(
                    modifier = Modifier
                        .widthIn(max = twoColumnsMaxWidth)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                    ) {
                        chip()
                        StatsSummaryHero(
                            state = state,
                            money = money,
                        )
                        StatsPeopleSection(
                            state = state,
                            money = money,
                            onEvent = onEvent,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                    ) {
                        StatsOverTimeSection(
                            state = state,
                            money = money,
                            dates = dates,
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

                else -> LazyColumn(
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
        }
    }
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
