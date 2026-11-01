package com.quare.blitzsplit.android.feature.groupstats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiEvent
import com.quare.blitzsplit.feature.groupstats.presentation.model.GroupStatsUiState

// Why: "Nada para mostrar ainda" with "Rachar conta", or a period without bills with "Ver toda a viagem".
@Composable
internal fun StatsEmptyCard(
    state: GroupStatsUiState,
    dates: StatsTextFormatter,
    onEvent: (GroupStatsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allStart = state.allStartMillis
    val allEnd = state.allEndMillis
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(28.dp))
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(percent = 40)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }
        if (state.isGroupEmpty || allStart == null || allEnd == null) {
            StatsEmptyMessage(
                title = stringResource(R.string.group_stats_empty_title),
                body = stringResource(R.string.group_stats_empty_body),
            )
            Button(
                onClick = { onEvent(GroupStatsUiEvent.OnSplitBillClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) { Text(stringResource(R.string.group_stats_split_bill)) }
        } else {
            StatsEmptyMessage(
                title = periodEmptyTitleOf(state, dates),
                body = stringResource(
                    R.string.group_stats_period_empty_body,
                    dates.toRangeText(state.allRangeStyle, allStart, allEnd),
                ),
            )
            FilledTonalButton(
                onClick = { onEvent(GroupStatsUiEvent.OnWholePeriodClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    stringResource(
                        if (state.isTrip) {
                            R.string.group_stats_see_whole_trip
                        } else {
                            R.string.group_stats_see_whole_period
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun StatsEmptyMessage(
    title: String,
    body: String,
) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 320.dp),
    )
}
